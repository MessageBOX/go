package com.ansteel.report.backup.fun;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ansteel.report.dc.domain.DataCollection;
import com.ansteel.report.dc.domain.DataCollectionTree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ansteel.common.backup.core.AbstractExecuteXml;
import com.ansteel.common.backup.core.IExecuteXml;
import com.ansteel.core.utils.BeanUtils;
import com.ansteel.core.utils.StringUtils;

/**
 * 创 建 人：gugu
 * 创建日期：2015-05-05
 * 修 改 人：
 * 修改日 期：
 * 描   述：Excel报表生成。 
 */
@Service
@Transactional(propagation=Propagation.REQUIRES_NEW)
public class DataCollectionExecute  extends AbstractExecuteXml implements IExecuteXml{

	@Override
	public Class getClazz() {
		return DataCollection.class;
	}


	@Override
	public void importSave(Map<Class, Object> xmlMap, Object o, int type) {
		//父节点
		Collection<DataCollectionTree> dataBaseResList = baseService.findAll(DataCollectionTree.class);
		Map<String, DataCollectionTree> dataBaseResMap = new HashMap<String, DataCollectionTree>();
		for(DataCollectionTree entity :dataBaseResList){
			dataBaseResMap.put(entity.getName(), entity);
		}

		List<DataCollectionTree> xmlParentList=(List<DataCollectionTree>) xmlMap.get(DataCollectionTreeExecute.class);
		Map<String, DataCollectionTree> parentXmlToDataBaseMap = new HashMap<String, DataCollectionTree>();
		for(DataCollectionTree entity :xmlParentList){
			String name = entity.getName();
			if(dataBaseResMap.containsKey(name)){
				if(StringUtils.hasText(name)){
					parentXmlToDataBaseMap.put(entity.getId(), dataBaseResMap.get(name));
				}
			}
		}
		//父节点
		

		List<DataCollection> xmlEntityList = (List<DataCollection>) o;
		Collection<DataCollection> dataBaseList = baseService.findAll(DataCollection.class);
		for (DataCollection entity : xmlEntityList) {
			DataCollection xmlEntity =new DataCollection();
			try {
				BeanUtils.copyProperties(xmlEntity, entity);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String xmlName = xmlEntity.getName();
			//此节点只有ID
			String xmlParentId=entity.getDataCollectionTree().getId();
			DataCollectionTree parentXmlToDataBaseEntity=null;
			if(StringUtils.hasText(xmlParentId)){
				if(parentXmlToDataBaseMap.containsKey(xmlParentId)){
					parentXmlToDataBaseEntity=parentXmlToDataBaseMap.get(xmlParentId);
					xmlEntity.setDataCollectionTree(parentXmlToDataBaseEntity);
				}else{
					continue;
				}
			}else{
				continue;
			}
		
			//确认xml中是否有数据库中的实体
			boolean isSave = true;
			for (DataCollection dataBaseEntity : dataBaseList) {
				//数据库节点名称
				String dataBaseName = dataBaseEntity.getName();
				if(dataBaseName.equals(xmlName)){
					//数据库父节点名称
					String dataBaseParentName = dataBaseEntity.getDataCollectionTree()==null?"":dataBaseEntity.getDataCollectionTree().getName();
					String parentXmlName = parentXmlToDataBaseEntity==null?"":parentXmlToDataBaseEntity.getName();
					if(dataBaseParentName.equals(parentXmlName)){
					
						if (dataBaseEntity.getVersionPublish() != xmlEntity.getVersion()) {
							DataCollection parent = getUpdateEntity(dataBaseEntity, xmlEntity);
							baseService.save(parent);
						}
						isSave=false;
						break;
					}
				}
			}
			
			
			if (isSave) {	
				xmlEntity.setVersionPublish(xmlEntity.getVersion());
				baseService.save(xmlEntity);
			}
		}
		
		
	}
	
	private DataCollection getUpdateEntity(DataCollection databaseEntity, DataCollection entity) {
		String oldId = databaseEntity.getId();
		Long version = databaseEntity.getVersion();
		try {
			BeanUtils.copyProperties(databaseEntity, entity);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		databaseEntity.setId(oldId);
		databaseEntity.setVersion(version);
		databaseEntity.setVersionPublish(entity.getVersion());
		return databaseEntity;
	}



}
