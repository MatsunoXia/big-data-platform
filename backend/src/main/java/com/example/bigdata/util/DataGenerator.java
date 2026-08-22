package com.example.bigdata.util;

import com.example.bigdata.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试数据生成器
 * 生成逼真的电商订单数据
 */
public class DataGenerator {

    private static final String[] CATEGORIES = {"电子产品", "服饰鞋包", "食品饮料", "家居用品", "图书文具"};
    private static final String[] STATUSES = {"待付款", "已付款", "已发货", "已完成", "已取消"};
    private static final String[] PROVINCES = {
            "北京", "上海", "广东", "浙江", "江苏", "四川", "湖北", "山东", "河南", "福建",
            "湖南", "安徽", "辽宁", "重庆", "天津", "河北", "云南", "陕西", "贵州", "广西"
    };
    private static final String[][] CITIES = {
            {"北京"}, {"上海"}, {"广州", "深圳", "东莞", "佛山"},
            {"杭州", "宁波", "温州", "嘉兴"}, {"南京", "苏州", "无锡", "常州"},
            {"成都", "绵阳", "德阳"}, {"武汉", "宜昌", "襄阳"},
            {"济南", "青岛", "烟台"}, {"郑州", "洛阳", "开封"},
            {"福州", "厦门", "泉州"}, {"长沙", "株洲", "湘潭"},
            {"合肥", "芜湖", "蚌埠"}, {"沈阳", "大连", "鞍山"},
            {"重庆"}, {"天津"}, {"石家庄", "唐山", "保定"},
            {"昆明", "大理", "丽江"}, {"西安", "咸阳", "宝鸡"},
            {"贵阳", "遵义"}, {"南宁", "桂林", "柳州"}
    };
    private static final String[] LAST_NAMES = {
            "张", "王", "李", "赵", "刘", "陈", "杨", "黄", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "林", "何", "高", "罗"
    };
    private static final String[] FIRST_NAMES = {
            "伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "洋", "勇",
            "艳", "杰", "娟", "涛", "明", "超", "秀英", "华", "慧", "建华"
    };
    private static final String[] PRODUCT_PREFIX = {
            "旗舰", "经典", "轻奢", "简约", "复古", "潮流", "高端", "基础", "限定", "联名"
    };
    private static final String[] PRODUCT_NAMES = {
            "手机", "笔记本电脑", "平板电脑", "智能手表", "无线耳机",
            "T恤", "牛仔裤", "运动鞋", "外套", "连衣裙",
            "坚果礼盒", "牛奶", "巧克力", "咖啡", "茶叶",
            "台灯", "抱枕", "收纳盒", "花瓶", "挂画",
            "小说", "教材", "漫画", "杂志", "绘本"
    };

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成一条随机订单
     * @param index 序号（用于生成订单号）
     */
    public static Order randomOrder(long index) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Order order = new Order();
        order.setOrderNo(generateOrderNo(index));
        order.setUserId((long) (random.nextInt(1, 100001)));  // 10万用户
        order.setUserName(randomName());
        order.setProductName(randomProductName());
        order.setCategory(CATEGORIES[random.nextInt(CATEGORIES.length)]);
        order.setAmount(randomAmount());
        order.setQuantity(random.nextInt(1, 11));  // 1~10件
        order.setStatus(STATUSES[random.nextInt(STATUSES.length)]);

        int provinceIdx = random.nextInt(PROVINCES.length);
        order.setProvince(PROVINCES[provinceIdx]);
        order.setCity(CITIES[provinceIdx][random.nextInt(CITIES[provinceIdx].length)]);

        order.setCreateTime(randomDateTime());
        order.setUpdateTime(order.getCreateTime().plusDays(random.nextInt(0, 7)));
        order.setDeleted(0);

        return order;
    }

    /**
     * 生成订单号: ORD + 6位随机数 + 13位时间戳
     */
    private static String generateOrderNo(long index) {
        return String.format("ORD%06d%d", index % 1000000, System.nanoTime() % 10000000000000L);
    }

    private static String randomName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)] +
               FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }

    private static String randomProductName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return PRODUCT_PREFIX[random.nextInt(PRODUCT_PREFIX.length)] +
               PRODUCT_NAMES[random.nextInt(PRODUCT_NAMES.length)];
    }

    private static BigDecimal randomAmount() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // 金额范围: 9.90 ~ 9999.99
        double amount = 9.90 + random.nextDouble() * 9990.09;
        return BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static LocalDateTime randomDateTime() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // 时间范围: 2023-01-01 ~ 2025-12-31
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
        long seconds = java.time.Duration.between(start, end).getSeconds();
        long randomSeconds = random.nextLong(seconds);
        return start.plusSeconds(randomSeconds);
    }
}
