package com.cai.game.mj.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard<T extends AbstractMJTable> extends AbstractMJHandler<T> {
	protected int _seat_index;
	protected int _send_card_data;

	// 特殊发牌操作标识
	protected boolean specialType = false;

	// 发多少个牌
	protected int cardCount = 1;
	// 这次摸牌是不是因为杠牌
	protected boolean isGang = false;

	// 需要留到下局处理牌值
	protected int card;

	protected int _type;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	protected GangCardResult m_gangCardResult;

	/**
	 * 杠牌的提供者，暗杠为-1，接杠、碰杠为出牌人
	 */
	protected int gang_provide_index;

	public MJHandlerDispatchCard() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_card_count(int card_num, boolean isGang) {
		this.cardCount = card_num;
		this.isGang = isGang;
	}
	
	public void reset_card_count_hua(int seat_index, int type,int card_num){
		_seat_index = seat_index;
		_type = type;
		this.cardCount = card_num;
	}

	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
		cardCount = 1;
	}

	public void reset_status(int seat_index, int type, int provide_index, boolean is_gang) {
		_seat_index = seat_index;
		_type = type;
		gang_provide_index = provide_index;
		isGang = is_gang;
	}

	public void reset_status(int seat_index, int type, boolean specialType) {
		_seat_index = seat_index;
		_type = type;
		this.specialType = specialType;
	}

	public void reset_status(int seat_index, int type, int card) {
		_seat_index = seat_index;
		_type = type;
		this.card = card;
	}
	
	public void reset_status_send(int seat_index, int type, int card) {
		_seat_index = seat_index;
		_type = type;
		this._send_card_data = card;
	}

	@Override
	public void exe(T table) {
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(T table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// if (card == MJGameConstants.ZZ_MAGIC_CARD &&
		// table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_error("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_KAI);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(T table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		// 摸牌
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		return true;
	}

	public void other_deal(T table) {

	}
}
