package org.jeecg.modules.wms.inorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;

/**
 * @Description: 入库单主表
 * @Author: jeecg-boot
 * @Date:   2026-09-14
 * @Version: V1.0
 */
@Mapper
public interface WmsStockInOrdersMapper extends BaseMapper<WmsStockInOrders> {

}
