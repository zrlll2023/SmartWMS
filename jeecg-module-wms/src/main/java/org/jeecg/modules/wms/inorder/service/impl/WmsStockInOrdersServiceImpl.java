package org.jeecg.modules.wms.inorder.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrderItemsMapper;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrdersMapper;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrdersService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 入库单主表
 * @Author: jeecg-boot
 * @Date:   2026-09-14
 * @Version: V1.0
 */
@Service
public class WmsStockInOrdersServiceImpl extends ServiceImpl<WmsStockInOrdersMapper, WmsStockInOrders> implements IWmsStockInOrdersService {

	@Autowired
	private WmsStockInOrdersMapper wmsStockInOrdersMapper;

	@Autowired
	private WmsStockInOrderItemsMapper wmsStockInOrderItemsMapper;

	@Autowired
	private RedisUtil redisUtil;

	@Autowired
	private IWmsStockInOrderItemsService wmsStockInOrderItemsService;
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveMain(WmsStockInOrders wmsStockInOrders, List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
		wmsStockInOrdersMapper.insert(wmsStockInOrders);
		if(wmsStockInOrderItemsList!=null && wmsStockInOrderItemsList.size()>0) {
			for(WmsStockInOrderItems entity:wmsStockInOrderItemsList) {
				//外键设置
				entity.setOrderId(wmsStockInOrders.getId());
				wmsStockInOrderItemsMapper.insert(entity);
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateMain(WmsStockInOrders wmsStockInOrders,List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
		//初始状态、审核失败状态可以修改
		if(!(WarehouseDictEnum.INBOUND_INITIAL.getCode().equals(wmsStockInOrders.getStatus())
				|| WarehouseDictEnum.INBOUND_REJECTED.getCode().equals(wmsStockInOrders.getStatus()))){
			throw new JeecgBootException("非初始状态、审核失败状态入库单不允许修改");
		}

		if(wmsStockInOrderItemsList==null || wmsStockInOrderItemsList.size()<=0){
			//抛出异常
			throw new JeecgBootException("请选择入库单明细");
		}
		//删除入库单明细数据
		wmsStockInOrderItemsMapper.deleteByMainId(wmsStockInOrders.getId());
		//对子表数据按商品分组,使用Stream流实现
		Map<String, List<WmsStockInOrderItems>> collect = wmsStockInOrderItemsList.stream().collect(Collectors.groupingBy(WmsStockInOrderItems::getProductId));
		//对List<WmsStockInOrderItems>进行合并
		List<WmsStockInOrderItems> mergeList = new ArrayList<>();
		collect.entrySet().stream().forEach(entry -> {
			List<WmsStockInOrderItems> value = entry.getValue();
			WmsStockInOrderItems merge = value.get(0);
			//明细的状态为初始状态
			merge.setStatus(WarehouseDictEnum.INBOUND_DETAIL_INITIAL.getCode());
			//入库单主表id
			merge.setOrderId(wmsStockInOrders.getId());
			if(value!=null && value.size()>1){
				merge.setExpectedQuantity(value.stream().mapToInt(WmsStockInOrderItems::getExpectedQuantity).sum());
				mergeList.add( merge);
			}else{
				mergeList.add(merge);
			}
		});

		int sum = mergeList.stream().mapToInt(WmsStockInOrderItems::getExpectedQuantity).sum();
		wmsStockInOrders.setTotalExpectedQuantity(sum);

		//添加子表数据
		boolean b = wmsStockInOrderItemsService.saveBatch(mergeList);
		if(!b){
			throw new JeecgBootException("保存入库单明细失败");
		}
		//更新入库单
		int i = wmsStockInOrdersMapper.updateById(wmsStockInOrders);
		if(i<=0){
			throw new JeecgBootException("更新入库单失败");
		}

	}
//	@Override
//	@Transactional(rollbackFor = Exception.class)
//	public void updateMain(WmsStockInOrders wmsStockInOrders,List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
//		wmsStockInOrdersMapper.updateById(wmsStockInOrders);
//
//		//1.先删除子表数据
//		wmsStockInOrderItemsMapper.deleteByMainId(wmsStockInOrders.getId());
//
//		//2.子表数据重新插入
//		if(wmsStockInOrderItemsList!=null && wmsStockInOrderItemsList.size()>0) {
//			for(WmsStockInOrderItems entity:wmsStockInOrderItemsList) {
//				//外键设置
//				entity.setOrderId(wmsStockInOrders.getId());
//				wmsStockInOrderItemsMapper.insert(entity);
//			}
//		}
//	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delMain(String id) {
		wmsStockInOrderItemsMapper.deleteByMainId(id);
		wmsStockInOrdersMapper.deleteById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delBatchMain(Collection<? extends Serializable> idList) {
		for(Serializable id:idList) {
			wmsStockInOrderItemsMapper.deleteByMainId(id.toString());
			wmsStockInOrdersMapper.deleteById(id);
		}
	}

	/**
	 * 添加入库单
	 */
	public void add(WmsStockInOrders wmsStockInOrders){
		//生成入库单号
		String orderNumber = generateOrderNumber();
		wmsStockInOrders.setOrderNumber(orderNumber);
		//默认状态为初始状态
		wmsStockInOrders.setStatus(WarehouseDictEnum.INBOUND_INITIAL.getCode());
		wmsStockInOrdersMapper.insert(wmsStockInOrders);
	}

	public String generateOrderNumber() {
		//当前8位时间戳(年月日), DateUtils.now()结果示例2001-11-11
		String time = DateUtils.now().substring(0, 10).replace("-", "");
		//key
		String key = "wms:asn_number"+time;
		long incr = redisUtil.incr(key, 1);
		if(incr == 1){
			//设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
			redisUtil.expire(key, 24*60*60+10);
		}
		//将incr组成4位字符串
		String incrStr = String.format("%04d", incr);
		String orderNumber = "ASN"+time+incrStr;

		return orderNumber;
	}

}
