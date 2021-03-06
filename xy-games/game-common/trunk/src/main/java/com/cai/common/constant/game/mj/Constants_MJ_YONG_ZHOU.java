package com.cai.common.constant.game.mj;


public class Constants_MJ_YONG_ZHOU {
    
	public static final int GAME_RULE_ZI_MO_HU = 1; //  自摸胡
	public static final int GAME_RULE_DIAN_PAO_HU = 2; // 点炮胡
	public static final int GAME_RULE_112_CARD = 3; // 112张
	public static final int GAME_RULE_136_CARD = 4; // 136张
	public static final int GAME_RULE_ALL_OPEN = 5;// 全开放
	public static final int GAME_RULE_HALF_OPEN = 6;// 半开放
	public static final int GAME_RULE_NO_OPEN = 7; // 不开放
	public static final int GAME_RULE_CATCH_ONE_BIRD = 8; // 抓1鸟
	public static final int GAME_RULE_CATCH_TWO_BIRD = 9; // 抓2鸟
	public static final int GAME_RULE_CATCH_FOUR_BIRD = 10; // 抓4鸟
	public static final int GAME_RULE_CATCH_SIX_BIRD = 11; // 抓6鸟
	public static final int GAME_RULE_ONE_BIRD_ALL_ZHONG = 12; // 一马全中
	public static final int GAME_RULE_NO_CATCH_BIRD = 13; // 不抓鸟
	public static final int GAME_RULE_CAN_EAT_CARD = 14; // 可以吃牌
	
    
    public static final int HU_CARD_TYPE_ZI_MO = 8; // 自摸
    public static final int HU_CARD_TYPE_DIAN_PAO = 9; // 点炮胡
    public static final int HU_CARD_TYPE_QIANG_GANG_HU = 10; // 抢杠胡
    public static final int HU_CARD_TYPE_GSKH = 11; // 杠上开花
    
    public static final int CHR_ZI_MO 				= 0x00000001; // 自摸
    public static final int CHR_DIAN_PAO 			= 0x00000002; // 点炮
    public static final int CHR_QIANG_GANG 			= 0x00000004; // 抢杠胡
    public static final int CHR_PING_HU 			= 0x00000008; // 平胡
    public static final int CHR_BEI_QIANG_GANG 		= 0x00000010; // 被抢杠
    public static final int CHR_QING_YI_SE 			= 0x00000020; // 清一色
    public static final int CHR_QI_QIAO_DUI 		= 0x00000040; // 七巧对
    public static final int CHR_SHI_SAN_LAN 		= 0x00000080; // 十三烂
    public static final int CHR_FANG_PAO 			= 0x00000100; // 放炮
    public static final int CHR_QI_FENG_DAO_WEI 	= 0x00000200; // 七风到位
    public static final int CHR_YING_QI_QIAO_DUI 	= 0x00000400; // 硬巧对
    public static final int CHR_PENG_PENG_HU 		= 0x00000800; // 碰碰胡
    
    public static final int CHR_WANG_DIAO 			= 0x00001000; // 王钓
    public static final int CHR_WANG_DIAO_WAN 		= 0x00002000; // 王钓王
    public static final int CHR_WANG_CHUANG			= 0x00004000; // 王闯
    public static final int CHR_WANG_CHUANG_WAN 	= 0x00008000; // 王闯王
    public static final int CHR_WANG_GUI_WEI 		= 0x00010000; // 王归位
    public static final int CHR_GS_WANG_DIAO 		= 0x00020000; // 杠上王钓
    public static final int CHR_GS_WANG_CHUANG 		= 0x00040000; // 杠上王闯
    public static final int CHR_GS_KAI_HUA			= 0x00080000; // 杠上开花
    
    
    
    public static final int HONG_ZHONG_DATA = 0x35; // 红中
	public static final int HONG_ZHONG_INDEX = 31; // 索引
    
	// 红中耗子
	public static final int CARD_DATA_HONG_ZHONG[] = new int[] { 
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
		0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
		0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
		
		0x35, 0x35, 0x35, 0x35, // 红中
	};
    
    public static final int CARD_DATA_DAI_FENG[] = new int[] { 
		0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, // 万子
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, // 索子
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, // 同子

        0x31, 0x31, 0x31, 0x31, // 东风
		0x32, 0x32, 0x32, 0x32, // 南风
		0x33, 0x33, 0x33, 0x33, // 西风
		0x34, 0x34, 0x34, 0x34, // 北风
		0x35, 0x35, 0x35, 0x35, // 红中
		0x36, 0x36, 0x36, 0x36, // 绿发
		0x37, 0x37, 0x37, 0x37, // 白板
    };
}
