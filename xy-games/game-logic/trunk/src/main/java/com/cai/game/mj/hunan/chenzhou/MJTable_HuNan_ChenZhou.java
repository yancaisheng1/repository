package com.cai.game.mj.hunan.chenzhou;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_HuNan_ChenZhou extends AbstractMJTable {

	private static final long serialVersionUID = -5573878136860256778L;

	protected MJHandlerQiShouHongZhong_HuNan_ChenZhou _handler_qishou_hongzhong;
	protected MJHandlerPiao_HuNan_ChenZhou _handler_piao;

	public MJTable_HuNan_ChenZhou(MJType game_type) {
		super(game_type);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if ((has_rule(GameConstants.GAME_RULE_HUNAN_ZIMOHU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))) {
			return GameConstants.WIK_NULL;
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_QIDUI)) { // 如果有可胡七对的玩法
			int check_qi_xiao_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

			if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
				if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
				}
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
				}
			}
		}

		if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 如果有红中玩法
			if ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 4)
					|| ((cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 3) && (cur_card == GameConstants.HZ_MAGIC_CARD))) { // 如果摸上了4个红中
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
				} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
				}
			}
		}

		if (!chiHuRight.is_empty()) { // 如果七小对胡牌或者4红中胡牌
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_WU_MAGIC_ADD_FAN)
					&& cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 0 && cur_card != GameConstants.HZ_MAGIC_CARD) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_WU_HZ_HU);
			}
			return cbChiHuKind;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 分析扑克
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		if (!bValue) { // 如果手中的牌不是胡牌的牌型
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean checkPengHu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		if (checkPengHu)
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) { // 自摸
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) { // 抢杠
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); // 点炮
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_WU_MAGIC_ADD_FAN)
				&& cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 0 && cur_card != GameConstants.HZ_MAGIC_CARD) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_WU_HZ_HU);
		}
		return cbChiHuKind;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round() && _logic.magic_count(GRR._cards_index[i]) == 0) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG,
						i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && _logic.magic_count(GRR._cards_index[i]) == 0) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public boolean exe_qishou_hongzhong(int seat_index) {
		this.set_handler(this._handler_qishou_hongzhong);
		this._handler_qishou_hongzhong.reset_status(seat_index);
		this._handler_qishou_hongzhong.exe(this);

		return true;
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_THREE)) {
			return 3; // 三人场
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_TWO)) {
			return 2; // 二人场
		} else {
			return 4; // 四人场
		}
	}

	private int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) { // 四人场
			seat = (seat_index + (nValue - 1) % 4) % 4;
		} else { // 三人场，所有胡牌人的对家都是那个空位置
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		// 如果有红中癞子的玩法，是不需要判断红中的
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
			if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)
					&& cards_index[this._logic.get_magic_card_index(0)] == 3) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, GameConstants.HZ_MAGIC_CARD, chr,
						GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
					cards[count] = GameConstants.HZ_MAGIC_CARD;
					count++;
				}
			}
		} else if (count > 0 && count < max_ting_count) {
			if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
				// 有胡的牌，红中肯定能胡
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			}
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 112张
			_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
		} else { // 108张
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_HuNan_ChenZhou();
		_handler_dispath_card = new MJHandlerDispatchCard_HuNan_ChenZhou();
		_handler_gang = new MJHandlerGang_HuNan_ChenZhou();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HuNan_ChenZhou();
		_handler_qishou_hongzhong = new MJHandlerQiShouHongZhong_HuNan_ChenZhou();

		_handler_piao = new MJHandlerPiao_HuNan_ChenZhou();

		if (has_rule(GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 红中癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
		}
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]); // 放炮的人？
				game_end.addGangScore(lGangScore[i]); // 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW; // 流局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		// 错误断言
		return false;
	}

	@Override
	protected boolean on_game_start() {
		if (!GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_NON_PIAO_FEN)) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0; // 清掉 默认是 -1
			}
		}

		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		if (GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 起手4个红中
				if (GRR._cards_index[i][_logic.get_magic_card_index(0)] == 4) {

					_playerStatus[i].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[i].add_zi_mo(_logic.switch_to_card_data(_logic.get_magic_card_index(0)), i);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_ZI_MO);
					GRR._chi_hu_rights[i].opr_or(GameConstants.CHR_HUNAN_HZ_QISHOU_HU);

					is_qishou_hu = true;
					this.exe_qishou_hongzhong(i);

					MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.hongZhong4, "", 0, 0l, this.getRoom_id());
					break;
				}
			}
		}

		if (is_qishou_hu == false) {
			this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	// type: 0自摸胡，1抢杠胡，2点炮胡
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int type) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		if (type == 0 || type == 3) { // 自摸和抢杠都算2分
			wFanShu = 2;
		}

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_DA_HU_FENG_DING)) {
			if (!chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE).is_empty())
				lChiHuScore = 10;
			if (!chr.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU).is_empty())
				lChiHuScore = 10;
			if (!chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI).is_empty())
				lChiHuScore = 10;
			if (!chr.opr_and(GameConstants.CHR_HUNAN_HZ_QISHOU_HU).is_empty())
				lChiHuScore = 10;
		}
		// 算基础分
		if (type == 0) { // 自摸--每人2分
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else if (type == 1) { // 抢杠胡--被抢杠者输6分，抢杠者赢6分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu * 3;
		} else { // 点炮--放炮者输一分，接炮者赢一分
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算奖码分 -- 杠分在开杠时就算进去了
		if (type == 0) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;
				if (!chr.opr_and(GameConstants.CHR_HUNAN_WU_HZ_HU).is_empty() && s < 10) {
					s *= 2;
				}

				s += GRR._count_pick_niao;

				s += this._player_result.pao[i] + this._player_result.pao[seat_index];

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else if (type == 3) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;
				if (!chr.opr_and(GameConstants.CHR_HUNAN_WU_HZ_HU).is_empty() && s < 10) {
					s *= 2;
				}

				s += GRR._count_pick_niao;

				s += this._player_result.pao[i] + this._player_result.pao[seat_index];

				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_GANG_BAO_QUAN_BAO)) {
					GRR._game_score[GRR._weave_items[seat_index][GRR._weave_count[seat_index] - 1].provide_player] -= s;
				} else {
					GRR._game_score[i] -= s;
				}
				GRR._game_score[seat_index] += s;
			}
		} else if (type == 1) {
			int s = lChiHuScore; // 抢杠2分，被抢杠者输6分，抢杠者赢6分，并把分加到游戏总分里
			if (!chr.opr_and(GameConstants.CHR_HUNAN_WU_HZ_HU).is_empty() && s < 10) {
				s *= 2;
			}
			s = s * 3;

			if (s > 10)
				s = 10;
			// s += 3; // 抢杠胡多给三分

			s += GRR._count_pick_niao;

			s += this._player_result.pao[provide_index] + this._player_result.pao[seat_index];

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._win_order[provide_index] = -1;
			GRR._chi_hu_rights[seat_index].opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU); // 抢杠胡
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO); // 放炮
		} else {
			int s = lChiHuScore; // 点炮1分，并把分加到游戏总分里
			if (!chr.opr_and(GameConstants.CHR_HUNAN_WU_HZ_HU).is_empty() && s < 10) {
				s *= 2;
			}
			s += GRR._count_pick_niao;

			s += this._player_result.pao[provide_index] + this._player_result.pao[seat_index];

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._win_order[provide_index] = -1;
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO); // 放炮
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x18, 0x18, 0x17, 0x17, 0x17 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x18, 0x18, 0x17, 0x17, 0x17 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x18, 0x18, 0x17, 0x17, 0x17 };
		int[] cards_of_player3 = new int[] { 0x18, 0x18, 0x17, 0x17, 0x19, 0x19, 0x19, 0x5, 0x5, 0x4, 0x4, 0x3, 0x3 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (this.getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}
		}
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中鸟X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中鸟X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");

						if (GRR._count_pick_niao > 0) {
							gameDesc.append(" 中鸟X" + GRR._count_pick_niao);
						}
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						gameDesc.append(" 碰碰胡");
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_HZ_QISHOU_HU) {
						gameDesc.append(" 四红中");
					}
					if (type == GameConstants.CHR_HUNAN_WU_HZ_HU) {
						gameDesc.append(" 无红中加倍");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}
			if (_player_result.pao[player] > 0) { // 显示飘分
				gameDesc.append(" 飘" + _player_result.pao[player] + "分");
			}

			GRR._result_des[player] = gameDesc.toString();
		}

	}

	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);
		if (weave_count != 0)
			this.load_player_info_data(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = this.get_niao_card_num();
		if (GRR._count_niao == 0) {
			return;
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			if (_logic.is_magic_card(GRR._cards_data_niao[i])) { // 如果是红中
				GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat_index]++;
			} else {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				// int seat = this.get_seat(nValue, seat_index);
				int seat = (seat_index + (nValue - 1) + 4) % 4; // 无论几个人打，只算159中
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_FEI_NIAO)) {
					seat = seat_index;
				}
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		GRR._count_pick_niao = this.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		boolean is_jing_niao = GRR._count_pick_niao == GRR._player_niao_count[seat_index] ? false : true;

		// 设置鸟牌显示
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (i == seat_index) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true, is_jing_niao);
				}
			} else {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false, is_jing_niao);
				}
			}
		}

		// 中鸟个数
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_FEI_NIAO)) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[0]);
			if (nValue == 1) {
				GRR._count_pick_niao = 10;
			} else {
				GRR._count_pick_niao = nValue;
			}
			if (GRR._cards_data_niao[0] == GameConstants.HZ_MAGIC_CARD) {
				GRR._count_pick_niao = 10;
			}
		}
	}

	public int set_ding_niao_valid(int card_data, boolean val, boolean is_jing_niao) {
		// 先把值还原
		if (is_jing_niao) {
			if (card_data > GameConstants.JING_NIAO_VALID) {
				card_data -= GameConstants.JING_NIAO_VALID;
			}
		} else {
			if (val) {
				if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
					card_data -= GameConstants.DING_NIAO_INVALID;
				} else if (card_data > GameConstants.DING_NIAO_VALID) {
					card_data -= GameConstants.DING_NIAO_VALID;
				}
			} else {
				if (card_data > GameConstants.DING_NIAO_INVALID) {
					return card_data;
				}
			}
		}

		if (is_jing_niao) {
			return (card_data < GameConstants.JING_NIAO_VALID ? card_data + GameConstants.JING_NIAO_VALID : card_data);
		} else {
			if (val == true) {
				// 生效
				return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
			} else {
				return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
			}
		}
	}

	private int get_niao_card_num() {
		int nNum = GameConstants.ZHANIAO_0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO2)) {// 奖2码
			nNum = GameConstants.ZHANIAO_2;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO4)) {// 奖4码
			nNum = GameConstants.ZHANIAO_4;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_ZHANIAO6)) {// 奖6码
			nNum = GameConstants.ZHANIAO_6;
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_FEI_NIAO)) {// 飞鸟
			nNum = GameConstants.ZHANIAO_1;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	private int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;

		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
				return 0;

			int nValue = _logic.get_card_value(cards_data[i]);

			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}
		}

		if (cbPickNum == 0 && GameDescUtil.has_rule(_game_rule_index, GameConstants.GAME_RULE_HUNAN_JINNIAO)) { // 有金鸟玩法并且一个没中时，算全中
			cbPickNum = card_num;
		}

		return cbPickNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
