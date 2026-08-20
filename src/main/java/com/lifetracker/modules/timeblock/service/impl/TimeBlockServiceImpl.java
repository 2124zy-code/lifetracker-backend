package com.lifetracker.modules.timeblock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifetracker.common.BusinessException;
import com.lifetracker.common.ErrorCodeEnum;
import com.lifetracker.common.UserContext;
import com.lifetracker.modules.timeblock.dto.BatchSaveTimeBlockDTO;
import com.lifetracker.modules.timeblock.dto.TimeBlockItemDTO;
import com.lifetracker.modules.timeblock.entity.UserTimeblock;
import com.lifetracker.modules.timeblock.mapper.UserTimeblockMapper;
import com.lifetracker.modules.timeblock.service.TimeBlockService;
import com.lifetracker.modules.timeblock.vo.TimeBlockCategoryStatVO;
import com.lifetracker.modules.timeblock.vo.TimeBlockDayVO;
import com.lifetracker.modules.timeblock.vo.TimeBlockItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TimeBlockServiceImpl implements TimeBlockService {

    private final UserTimeblockMapper timeblockMapper;

    private static final Map<String, String> CATEGORY_NAMES = new HashMap<>();
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<>();

    static {
        CATEGORY_NAMES.put("WORK", "深度工作");
        CATEGORY_NAMES.put("STUDY", "学习充电");
        CATEGORY_NAMES.put("SPORT", "运动健身");
        CATEGORY_NAMES.put("REST", "休闲放松");
        CATEGORY_NAMES.put("SLEEP", "睡眠休息");
        CATEGORY_NAMES.put("EMPTY", "未分配");

        CATEGORY_COLORS.put("WORK", "#8B5CF6");
        CATEGORY_COLORS.put("STUDY", "#3B82F6");
        CATEGORY_COLORS.put("SPORT", "#10B981");
        CATEGORY_COLORS.put("REST", "#F59E0B");
        CATEGORY_COLORS.put("SLEEP", "#6366F1");
        CATEGORY_COLORS.put("EMPTY", "#1E293B");
    }

    public TimeBlockServiceImpl(UserTimeblockMapper timeblockMapper) {
        this.timeblockMapper = timeblockMapper;
    }

    @Override
    public TimeBlockDayVO getDayTimeBlocks(LocalDate date) {
        Long userId = UserContext.getRequiredUserId();
        LocalDate queryDate = date != null ? date : LocalDate.now();
        log.info("[TimeBlockModule] 获取指定日期48格时间块: userId={}, date={}", userId, queryDate);

        List<UserTimeblock> records = timeblockMapper.selectList(new LambdaQueryWrapper<UserTimeblock>()
                .eq(UserTimeblock::getUserId, userId)
                .eq(UserTimeblock::getRecordDate, queryDate)
                .orderByAsc(UserTimeblock::getBlockIndex));

        Map<Integer, UserTimeblock> recordMap = records.stream()
                .collect(Collectors.toMap(UserTimeblock::getBlockIndex, r -> r, (k1, k2) -> k1));

        // 填充完整的 48 格 (0 ~ 47)
        List<TimeBlockItemVO> blockList = new ArrayList<>(48);
        Map<String, Integer> categoryCountMap = new LinkedHashMap<>();
        categoryCountMap.put("WORK", 0);
        categoryCountMap.put("STUDY", 0);
        categoryCountMap.put("SPORT", 0);
        categoryCountMap.put("REST", 0);
        categoryCountMap.put("SLEEP", 0);

        int recordedCount = 0;
        for (int i = 0; i < 48; i++) {
            UserTimeblock record = recordMap.get(i);
            String category = (record != null && record.getCategory() != null) ? record.getCategory().toUpperCase() : "EMPTY";
            String note = (record != null) ? record.getNote() : "";

            if (!"EMPTY".equals(category) && categoryCountMap.containsKey(category)) {
                categoryCountMap.put(category, categoryCountMap.get(category) + 1);
                recordedCount++;
            }

            blockList.add(TimeBlockItemVO.builder()
                    .blockIndex(i)
                    .timeRange(formatTimeRange(i))
                    .category(category)
                    .categoryName(CATEGORY_NAMES.getOrDefault(category, "未分配"))
                    .color(CATEGORY_COLORS.getOrDefault(category, "#1E293B"))
                    .note(note)
                    .build());
        }

        // 计算分类统计与玫瑰图占比
        List<TimeBlockCategoryStatVO> stats = new ArrayList<>();
        double focusHours = (categoryCountMap.get("WORK") + categoryCountMap.get("STUDY")) * 0.5;

        for (Map.Entry<String, Integer> entry : categoryCountMap.entrySet()) {
            String cat = entry.getKey();
            int count = entry.getValue();
            double hours = count * 0.5;
            double percentage = recordedCount > 0 ? Math.round((count * 100.0 / 48) * 10.0) / 10.0 : 0.0;

            stats.add(TimeBlockCategoryStatVO.builder()
                    .category(cat)
                    .categoryName(CATEGORY_NAMES.get(cat))
                    .color(CATEGORY_COLORS.get(cat))
                    .blockCount(count)
                    .totalHours(hours)
                    .percentage(percentage)
                    .build());
        }

        return TimeBlockDayVO.builder()
                .date(queryDate)
                .totalRecordedBlocks(recordedCount)
                .totalFocusHours(focusHours)
                .blocks(blockList)
                .categoryStats(stats)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TimeBlockDayVO saveBatch(BatchSaveTimeBlockDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        LocalDate recordDate;
        try {
            recordDate = LocalDate.parse(dto.getDate());
        } catch (Exception e) {
            log.warn("[TimeBlockModule] 日期解析失败: date={}", dto.getDate());
            throw new BusinessException(ErrorCodeEnum.PARAM_INVALID, "日期格式错误，应为 YYYY-MM-DD");
        }

        log.info("[TimeBlockModule] 批量保存时间块: userId={}, date={}, blocksCount={}", 
                userId, recordDate, dto.getBlocks().size());

        for (TimeBlockItemDTO item : dto.getBlocks()) {
            if (item.getBlockIndex() < 0 || item.getBlockIndex() > 47) {
                throw new BusinessException(ErrorCodeEnum.TIMEBLOCK_INDEX_INVALID);
            }

            // 删除原位置已存在记录
            timeblockMapper.delete(new LambdaQueryWrapper<UserTimeblock>()
                    .eq(UserTimeblock::getUserId, userId)
                    .eq(UserTimeblock::getRecordDate, recordDate)
                    .eq(UserTimeblock::getBlockIndex, item.getBlockIndex()));

            if (!"EMPTY".equalsIgnoreCase(item.getCategory())) {
                UserTimeblock tb = UserTimeblock.builder()
                        .userId(userId)
                        .recordDate(recordDate)
                        .blockIndex(item.getBlockIndex())
                        .category(item.getCategory().toUpperCase())
                        .note(item.getNote() != null ? item.getNote().trim() : "")
                        .createdAt(LocalDateTime.now())
                        .build();
                timeblockMapper.insert(tb);
            }
        }

        return getDayTimeBlocks(recordDate);
    }

    public static String formatTimeRange(int index) {
        int startMinutes = index * 30;
        int endMinutes = (index + 1) * 30;

        int startH = startMinutes / 60;
        int startM = startMinutes % 60;
        int endH = endMinutes / 60;
        int endM = endMinutes % 60;

        return String.format("%02d:%02d - %02d:%02d", startH, startM, endH, endM);
    }
}
