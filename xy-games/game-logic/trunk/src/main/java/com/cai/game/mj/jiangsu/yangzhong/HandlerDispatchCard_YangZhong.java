package com.cai.game.mj.jiangsu.yangzhong;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_YangZhong;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_YangZhong extends MJHandlerDispatchCard<Table_YangZhong> {
	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_YangZhong() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_YangZhong table) {
		table._card_can_not_out_after_chi[_seat_index] = 0;
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;

		// 摸牌
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x11;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int card_type = Constants_YangZhong.HU_CARD_TYPE_ZI_MO;
		// 杠上开花
		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			card_type = Constants_YangZhong.HU_CARD_TYPE_GANG_KAI;
		}

		// 判断胡牌
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
		if (action != GameConstants.WIK_NULL) {
			// 屁胡
			if (table.isNeedHua(_seat_index)) {
				if ((table.has_rule(Constants_YangZhong.GAME_RULE_YI_MO_ER_CHONG) && (table.player_di_fen[_seat_index] > 0))
						|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_ER_HUA) && table.player_di_fen[_seat_index] > 1)
						|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_SAN_HUA) && table.player_di_fen[_seat_index] > 2)
						|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_WU_HUA) && table.player_di_fen[_seat_index] > 4)) {
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
				}
			} else {
				// 非屁胡
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			}
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);

		while (table.out_hua_pai_count < 8) {
			// 花牌数量
			int hua_pai_count = table.get_hua_pai_count(table.GRR._cards_index[_seat_index]);
			if (hua_pai_count == 0) {
				break;
			}

			table.out_hua_pai_count += hua_pai_count;

			int hua_pai[] = new int[GameConstants.MAX_COUNT];
			int tmp_hua_pai_count = 0;

			for (int i = GameConstants.MAX_ZI_FENG; i < GameConstants.MAX_INDEX; i++) {
				if (table.GRR._cards_index[_seat_index][i] == 1) {
					table.GRR._player_niao_cards[_seat_index][table.GRR._player_niao_count[_seat_index]++] = table._logic.switch_to_card_data(i);
					table.GRR._cards_index[_seat_index][i] = 0;

					int card_value = table._logic.get_card_value(i);
					if (card_value > 5) {
						card_value -= 5;
					} else {
						card_value -= 1;
					}

					if (card_value == table.men_hua_men_feng[_seat_index])
						table.player_di_fen[_seat_index] += 2;
					else
						table.player_di_fen[_seat_index] += 1;

					hua_pai[tmp_hua_pai_count++] = table._logic.switch_to_card_data(i);
					table.operate_di_fen_bei_shu(_seat_index);
				}
			}

			// 判断胡牌
			int action1 = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
			if (action1 != GameConstants.WIK_NULL) {
				// 屁胡
				if (table.isNeedHua(_seat_index)) {
					if ((table.has_rule(Constants_YangZhong.GAME_RULE_YI_MO_ER_CHONG) && (table.player_di_fen[_seat_index] > 0))
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_ER_HUA) && table.player_di_fen[_seat_index] > 1)
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_SAN_HUA) && table.player_di_fen[_seat_index] > 2)
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_WU_HUA) && table.player_di_fen[_seat_index] > 4)) {
						curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
						curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
					}
				} else {
					// 非屁胡
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
				}
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}
			table._send_card_count += hua_pai_count;
			table.GRR._left_card_count -= hua_pai_count - 1;

			// 牌堆数大于0才补花
			int tmp_card_index[] = new int[GameConstants.MAX_INDEX];
			if (table.GRR._left_card_count != 0) {
				table._logic.switch_to_cards_index(table._repertory_card, table._all_card_len - table.GRR._left_card_count, hua_pai_count,
						tmp_card_index);
				table.GRR._left_card_count -= hua_pai_count;
			}

			// 判断胡牌
			int action2 = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);
			if (action2 != GameConstants.WIK_NULL) {
				// 屁胡
				if (table.isNeedHua(_seat_index)) {
					if ((table.has_rule(Constants_YangZhong.GAME_RULE_YI_MO_ER_CHONG) && (table.player_di_fen[_seat_index] > 0))
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_ER_HUA) && table.player_di_fen[_seat_index] > 1)
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_SAN_HUA) && table.player_di_fen[_seat_index] > 2)
							|| (table.has_rule(Constants_YangZhong.GAME_RULE_YING_WU_HUA) && table.player_di_fen[_seat_index] > 4)) {
						curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
						curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
					}
				} else {
					// 非屁胡
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
				}
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (tmp_card_index[i] > 0) {
					table.GRR._cards_index[_seat_index][i] += tmp_card_index[i];
					table.GRR.addHuaCard(_seat_index, table._logic.switch_to_card_data(i));
				}
			}

			// TODO: 在牌桌上显示出的花牌并刷新手牌
			table.operate_out_card(_seat_index, hua_pai_count, hua_pai, GameConstants.OUT_CARD_TYPE_HUA_PAI, GameConstants.INVALID_SEAT);

			table.exe_add_discard(_seat_index, hua_pai_count, hua_pai, false, 0);

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BU_HUA }, 1,
					GameConstants.INVALID_SEAT);

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, 0, null);
		}

		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_hong_zhong_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.GRR._cards_abandoned_gang[_seat_index]);

			if (cbActionMask != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			return;
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return;
	}

	@Override
	public boolean handler_player_out_card(Table_YangZhong table, int seat_index, int card) {
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

		table.exe_out_card(_seat_index, card, _type);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_YangZhong table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}

		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_YangZhong table, int seat_index) {
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		table.operate_di_fen_bei_shu(_seat_index);

		return true;
	}
}
