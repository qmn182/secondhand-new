// service/PointsRecordService.java
package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.example.demo.entity.PointsRecord;
import com.example.demo.mapper.PointsRecordMapper;

@Service
public class PointsRecordService extends ServiceImpl<PointsRecordMapper, PointsRecord> {
    public void record(Long userId, int points, int balance, String type, Long linkId, String remark) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(points);
        record.setBalance(balance);
        record.setType(type);
        record.setLinkId(linkId);
        record.setRemark(remark);
        this.save(record);
    }
}