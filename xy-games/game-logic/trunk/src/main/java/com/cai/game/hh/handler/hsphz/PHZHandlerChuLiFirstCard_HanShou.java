/**
 * 
 */
package com.cai.game.hh.handler.hsphz;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class PHZHandlerChuLiFirstCard_HanShou extends HHHandlerDispatchCard<HanShouHHTable> {

	@Override
	public void exe(HanShouHHTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}

		// 刷新手牌包括组合
		// table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		boolean ti_send_card = false;
		//// 玩家出牌 响应判断,是否有提 暗龙

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		// if (table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
		// // 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// // 胡牌判断
		// for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
		// if (table.GRR._cards_index[_seat_index][i] == 4) {
		// an_long_Index[an_long_count++] = i;
		// }
		// }
		// for (int i = 0; i < an_long_count; i++) {
		// int cbWeaveIndex = table.GRR._weave_count[_seat_index];
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card =
		// table._logic.switch_to_card_data(an_long_Index[i]);
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind =
		// GameConstants.WIK_AN_LONG;
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player =
		// _seat_index;
		// table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi =
		// table._logic
		// .get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
		// table.GRR._weave_count[_seat_index]++;
		// table._long_count[_seat_index]++;
		// // 删除手上的牌
		// table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
		//
		// table.GRR._card_count[_seat_index] =
		// table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		//
		// }
		// ChiHuRight chr[] = new ChiHuRight[3];
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// chr[i] = table.GRR._chi_hu_rights[i];
		// chr[i].set_empty();
		// }
		// int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		// int bHupai = 0;
		//
		// int action_hu[] = new int[3];
		//
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// int card_data = table._send_card_data;
		// if (table._current_player == i)
		// card_data = 0;
		// PlayerStatus tempPlayerStatus = table._playerStatus[i];
		// tempPlayerStatus.reset();
		// int hu_xi[] = new int[1];
		// action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i],
		// table.GRR._weave_items[i], table.GRR._weave_count[i], i,
		// _seat_index, card_data, chr[i], card_type, hu_xi, true);// 自摸
		// if (action_hu[i] != GameConstants.WIK_NULL) {
		// // 添加动作
		// tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
		// tempPlayerStatus.add_zi_mo(table._send_card_data, i);
		// tempPlayerStatus.add_action(GameConstants.WIK_NULL);
		// tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
		// table.operate_player_action(i, false);
		// bHupai = 1;
		// } else {
		// chr[i].set_empty();
		// }
		//
		// }
		// if (bHupai == 0) {
		//
		// if (an_long_count > 0) {
		// int _action = GameConstants.WIK_AN_LONG;
		// // 效果
		// table.operate_effect_action(_seat_index,
		// GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action },
		// 1,
		// GameConstants.INVALID_SEAT);
		//
		// // 刷新手牌包括组合
		//
		// hand_card_count =
		// table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],
		// cards);
		// table.operate_player_cards(_seat_index, hand_card_count, cards,
		// table.GRR._weave_count[_seat_index],
		// table.GRR._weave_items[_seat_index]);
		//
		// }
		//
		// boolean check = false;
		// if (an_long_count >= 2) {
		// table._ti_two_long[_seat_index] = true;
		// check = table.is_ting_state(table.GRR._cards_index[_seat_index],
		// table.GRR._weave_items[_seat_index],
		// table.GRR._weave_count[_seat_index], _seat_index);
		// }
		//
		// if (check) {
		// int next_player = (_seat_index + table.getTablePlayerNumber() + 1) %
		// table.getTablePlayerNumber();
		// table._current_player = next_player;
		// table._last_player = next_player;
		// table._last_card = 0;
		// table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 500);
		//
		// return;
		// }
		//
		// // 加到手牌
		//
		// hand_card_count =
		// table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],
		// cards);
		// table.operate_player_cards(_seat_index, hand_card_count, cards,
		// table.GRR._weave_count[_seat_index],
		// table.GRR._weave_items[_seat_index]);
		// table.operate_player_get_card(_seat_index, 0, null,
		// GameConstants.INVALID_SEAT, false);
		// curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);//
		// 出牌状态
		// table.operate_player_status();
		// // 发牌
		//
		// }
		//
		// } else {

		// ---------------------------- 2017-11-11 庄家提龙
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;

			}
		}
		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

			}
			// 刷新手牌包括组合
			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		}

		boolean check = false;
		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
			check = table.is_ting_state(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
					_seat_index);
		}
		// -------------------------------------------

		// ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		// chr.set_empty();
		// int action = 0;
		// int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		// if
		// (table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]
		// != 0) {
		// int hu_xi[] = new int[1];
		// action =
		// table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
		// table.GRR._weave_items[_seat_index],
		// table.GRR._weave_count[_seat_index], _seat_index, _seat_index,
		// table._send_card_data, chr, card_type, hu_xi, true);// 自摸
		// } else {
		// int hu_xi[] = new int[1];
		// table.operate_player_get_card(_seat_index, 0, null,
		// GameConstants.INVALID_SEAT, false);
		// action =
		// table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
		// table.GRR._weave_items[_seat_index],
		// table.GRR._weave_count[_seat_index], _seat_index, _seat_index, 0,
		// chr,
		// card_type, hu_xi, true);// 自摸
		// }

		ChiHuRight chrs[] = new ChiHuRight[3];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chrs[i] = table.GRR._chi_hu_rights[i];
			chrs[i].set_empty();
		}
		boolean haveTianHu = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus playerStatus = table._playerStatus[i];

			int cur_card = 0;
			if (i != _seat_index)
				cur_card = table._send_card_data;
			int hu_xi = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _seat_index,
					cur_card, chrs[i], GameConstants.HU_CARD_TYPE_TIAN_HU, hu_xi_chi, true);// 自

			if (hu_xi != GameConstants.WIK_NULL) {
				playerStatus.add_action(GameConstants.WIK_ZI_MO);
				playerStatus.add_zi_mo(table._send_card_data, i);
				playerStatus.add_action(GameConstants.WIK_NULL);
				playerStatus.add_pass(_send_card_data, _seat_index);

				// 发 操作
				table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(i, false);
				haveTianHu = true;
			}
		}
		if (haveTianHu) {
			// 添加动作
			// table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
			// table._playerStatus[_seat_index].add_zi_mo(table._send_card_data,
			// _seat_index);
			// table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
			// table._playerStatus[_seat_index].add_pass(table._send_card_data,
			// _seat_index);

		} else {
			if (check) {
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;
				table._last_card = 0;
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 500);

				table._ti_two_long[_seat_index] = false;
				return;
			}
			chrs[_seat_index].set_empty();
			// 加到手牌

			hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			table.fiter_ting(_seat_index);
			// 发牌
		}
		// }

		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(HanShouHHTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		// if (seat_index != _seat_index) {
		// table.log_error("DispatchCard 不是当前玩家操作");
		// return false;
		// }
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			boolean flag = false;
			for (int i = 0; i < playerStatus._action_count; i++) {

				switch (playerStatus._action[i]) {
				case GameConstants.WIK_LEFT:
				case GameConstants.WIK_CENTER:
				case GameConstants.WIK_RIGHT:
				case GameConstants.WIK_XXD:
				case GameConstants.WIK_DDX:
				case GameConstants.WIK_EQS:
					if (flag == false) {
						table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = operate_card;
						playerStatus.set_exe_pass(true);
						flag = true;
					}
					break;
				case GameConstants.WIK_PENG: {
					table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
					playerStatus.set_exe_pass(true);
				}
					break;
				}
			}

		}
		// 吃操作后，是否有落
		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
			if (luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[3];
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				}
			}
		}

		// 优先级最高的人还没操作

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_error("最用户操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
		// 判断可不可以吃的上家用户
		int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		boolean flag = false;
		for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {

			switch (table._playerStatus[last_player]._action[j]) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS:
				if (target_action == GameConstants.WIK_NULL)
					continue;
				if (flag == false)
					if (table._playerStatus[last_player].get_exe_pass() == true) {
						table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
						flag = true;
						table._playerStatus[last_player].set_exe_pass(false);
					}

				break;
			}
		}
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			boolean flag_temp = false;

			if (table._playerStatus[i].has_action()) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {

					switch (table._playerStatus[i]._action[j]) {
					case GameConstants.WIK_LEFT:
					case GameConstants.WIK_CENTER:
					case GameConstants.WIK_RIGHT:
					case GameConstants.WIK_XXD:
					case GameConstants.WIK_DDX:
					case GameConstants.WIK_EQS:
						if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO)))
							continue;
						if (flag_temp == false)
							if (table._playerStatus[i].get_exe_pass() == true) {
								table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
								flag_temp = true;
							}

						break;
					case GameConstants.WIK_PENG:
						if (!((target_action == GameConstants.WIK_NULL) || (target_action & eat_type) != GameConstants.WIK_NULL))
							continue;
						if (table._playerStatus[i].get_exe_pass() == false) {
							table._cannot_peng[i][table._cannot_peng_count[i]++] = operate_card;
						}
						break;
					}
				}
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL: {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			if (table._long_count[_seat_index] > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				// 效果
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
						GameConstants.INVALID_SEAT);

				// 刷新手牌包括组合
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
			}
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				// 要出牌，但是没有牌出设置成相公 下家用户发牌
				int pai_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}
				if (pai_count == 0) {
					table._is_xiang_gong[_seat_index] = true;
					table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					table.log_error(next_player + "可以胡，而不胡的情况 " + _seat_index);
					return true;
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.fiter_ting(_seat_index);
				table.operate_player_status();
			}
			return true;

		}

		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			// if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG))
			// {// 轮装
			// if (table.GRR._banker_player == target_player) {
			// table._banker_select = target_player;
			// } else {
			// table._banker_select = (table.GRR._banker_player +
			// table.getTablePlayerNumber() + 1)
			// % table.getTablePlayerNumber();
			// }
			// }
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}
			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HanShouHHTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		table.fiter_ting(seat_index);
		return true;
	}
}
