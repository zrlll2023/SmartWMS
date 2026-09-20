package org.jeecg.modules.wms.wmstask.service;

import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date:   2026-09-20
 * @Version: V1.0
 */
public interface IWmsTasksService extends IService<WmsTasks> {

    /**
     * 创建收获任务
     * @param orderId
     * @param operator
     */
    public void createReceviceTask(String orderId, String operator);
}



