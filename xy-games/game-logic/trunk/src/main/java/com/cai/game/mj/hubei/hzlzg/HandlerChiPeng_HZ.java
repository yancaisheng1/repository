package com.cai.game.mj.hubei.hzlzg;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerChiPeng_HZ extends MJHandlerChiPeng<Table_HZ> {
	protected int _seat_index;
	protected int _action;
	protected int _card;
	protected int _provider;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerChiPeng_HZ() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int provider, int action, int card, int type) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
	}

	@Override
	public void exe(Table_HZ table) {
		table.kai_kou[_seat_index] = true; // 开口了
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._current_player = _seat_index;

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		if (table.quan_bao[_seat_index] == -1
				&& (table.bao_pai[_seat_index] == GameConstants_HZLZG.BAO_QING || table.bao_pai[_seat_index] == GameConstants_HZLZG.BAO_JIANG))
			table.quan_bao[_seat_index] = _provider;

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();

		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > 0) {
			int cbActionMask = table._logic.analyse_gang_by_card_hand_card_hu_bei(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, 0);

			if (cbActionMask != 0) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		if (table.bao_pai[_seat_index] == 0
				&& GameConstants_HZLZG.WIK_NULL != table.check_bao_jiang(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]))
			curPlayerStatus.add_action(GameConstants_HZLZG.WIK_BAO_JIANG);
		if (table.bao_pai[_seat_index] == 0
				&& GameConstants_HZLZG.WIK_NULL != table.check_bao_qing(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]))
			curPlayerStatus.add_action(GameConstants_HZLZG.WIK_BAO_QING);

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(Table_HZ table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			if (table.bao_pai[_seat_index] == 0 && GameConstants_HZLZG.WIK_NULL != table.check_bao_jiang(table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index]))
				table.bao_pai[_seat_index] = GameConstants_HZLZG.BAO_GUO;
			if (table.bao_pai[_seat_index] == 0
					&& GameConstants_HZLZG.WIK_NULL != table.check_bao_qing(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]))
				table.bao_pai[_seat_index] = GameConstants_HZLZG.BAO_GUO;
			return true;
		}

		switch (operate_code) {
		case GameConstants_HZLZG.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		case GameConstants_HZLZG.WIK_BAO_JIANG: {
			table.bao_pai[_seat_index] = GameConstants_HZLZG.BAO_JIANG;
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1,
					GameConstants.INVALID_SEAT);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
			break;
		case GameConstants_HZLZG.WIK_BAO_QING: {
			table.bao_pai[_seat_index] = GameConstants_HZLZG.BAO_QING;
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1,
					GameConstants.INVALID_SEAT);

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
			break;
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_HZ table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 杠牌
		if (table.is_gang_card(card) || table._logic.is_magic_card(card)) {
			table.exe_lai_gang(_seat_index, _seat_index, card, 0, 0, true, false);
			return true;
		}

		table.exe_out_card(_seat_index, card, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_HZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.card_2_magic(table.GRR._discard_cards[i][j]));
			}
			tableResponse.addDiscardCards(int_array);

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

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i]))
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
