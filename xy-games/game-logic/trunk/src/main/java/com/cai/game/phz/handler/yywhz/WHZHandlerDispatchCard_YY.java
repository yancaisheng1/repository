package com.cai.game.phz.handler.yywhz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phz.handler.PHZHandlerDispatchCard;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class WHZHandlerDispatchCard_YY extends PHZHandlerDispatchCard<YYWHZTable> {

	@Override
	public void exe(YYWHZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table._playerStatus[i].reset();
		}

		// table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards,
						table.GRR._weave_items[i], table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.operate_dou_liu_zi(-1, false, 0);
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
					2, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index]._hu_card_count = table.get_yywhz_ting_card(
				table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, _seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x16;
		}
		--table.GRR._left_card_count;
		table._last_card = _send_card_data;

		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);

		// 用户是否歪
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int ti_wai = table.estimate_player_wai_respond_yywhz(_seat_index, _send_card_data);
		if (ti_wai != GameConstants.WIK_NULL) {

			if (table.has_rule(GameConstants.GAME_RULE_YYWHZ_KA_WAI)) {

				if (table.has_rule(GameConstants.GAME_RULE_YYWHZ_HU_FIRST)) {
					int action_hu = GameConstants.WIK_NULL;
					int hu_xi_chi[] = new int[1];
					hu_xi_chi[0] = 0;
					action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
							_seat_index, _send_card_data, table.GRR._chi_hu_rights[_seat_index], card_type, hu_xi_chi,
							true);// 自摸
					if (table._is_xiang_gong[_seat_index] == true)
						action_hu = GameConstants.WIK_NULL;
					if (action_hu != GameConstants.WIK_NULL) {
						// 歪
						curPlayerStatus.add_wai(_send_card_data, _seat_index, _send_card_data,
								GameConstants.WIK_YYWHZ_WAI);
						curPlayerStatus.add_action(GameConstants.WIK_YYWHZ_WAI);
						// 胡
						curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
						curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
						curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
						table.operate_player_action(_seat_index, false);
						ti_wai = GameConstants.WIK_ZI_MO;
					} else {
						table.GRR._chi_hu_rights[_seat_index].set_empty();
						table.exe_wai(_seat_index, _seat_index, ti_wai, _send_card_data,
								GameConstants.CHI_PENG_TYPE_DISPATCH, _send_card_data);
					}
				} else {
					table.exe_wai(_seat_index, _seat_index, ti_wai, _send_card_data,
							GameConstants.CHI_PENG_TYPE_DISPATCH, _send_card_data);
				}

				return;
			} else {
				curPlayerStatus.add_wai(_send_card_data, _seat_index, _send_card_data, GameConstants.WIK_YYWHZ_WAI);
				curPlayerStatus.add_action(GameConstants.WIK_YYWHZ_WAI);
				curPlayerStatus.add_action(GameConstants.WIK_NULL);
				curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(_seat_index, false);
			}
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断

		ChiHuRight chr[] = new ChiHuRight[3];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
		int bHupai = 0;

		int action_hu[] = new int[3];
		int action_pao[] = new int[3];

		int loop = 0;
		while (loop < table.getTablePlayerNumber()) {
			int i = (_seat_index + loop) % table.getTablePlayerNumber();
			loop++;
			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
					table.GRR._weave_count[i], i, _seat_index, _send_card_data, chr[i], card_type, hu_xi_chi, true);// 自摸
			if (table._is_xiang_gong[i] == true)
				action_hu[i] = GameConstants.WIK_NULL;
			if (action_hu[i] != GameConstants.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, i);
				tempPlayerStatus.add_action(GameConstants.WIK_NULL);
				tempPlayerStatus.add_pass(_send_card_data, i);
				ti_wai = GameConstants.WIK_ZI_MO;
				bHupai = 1;
			} else {
				chr[i].set_empty();
			}

		}

		// 玩家出牌 响应判断,是否有吃,碰,胡 swe
		boolean bAroseAction = false;
		bAroseAction = table.estimate_player_out_card_respond_yywhz(_seat_index, _send_card_data,
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], true);

		// 玩家溜
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask = table.estimate_player_liu_wai_respond_yywhz(table.GRR._cards_index[_seat_index],
				table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data,
				gangCardResult, true);
		if (cbActionMask != GameConstants.WIK_NULL) {// 有溜
			for (int i = 0; i < gangCardResult.cbCardCount; i++) {
				curPlayerStatus.add_liu(gangCardResult.cbCardData[i], _seat_index, gangCardResult.isPublic[i],
						cbActionMask);
				curPlayerStatus.add_action(cbActionMask);// 溜牌
				curPlayerStatus.add_action(GameConstants.WIK_NULL);
				curPlayerStatus.add_pass(_send_card_data, _seat_index);

				curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态

			}
		}

		if ((bAroseAction == false) && (ti_wai == GameConstants.WIK_NULL)) {
			table.operate_player_action(_seat_index, true);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//
					// 操作状态
					table.operate_player_action(i, false);
				}

			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
			table.log_error(_seat_index + "操作状态" + bAroseAction);
		} else {
			if (table.isTrutess(_seat_index)) {

				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			if (ti_wai == GameConstants.WIK_NULL) {

				if (bAroseAction == false) {

					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
					// table.operate_remove_discard(table._current_player,
					// table.GRR._discard_count[table._current_player]);
					// 没有人要就加入到牌堆
					int discard_time = 2000;
					int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
					SysParamModel sysParamModel1104 = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(gameId).get(1104);
					if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0
							&& sysParamModel1104.getVal4() < 10000) {
						discard_time = sysParamModel1104.getVal4();
					}

					if (table._last_card != 0)
						table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, discard_time);

					// 显示出牌
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 过张的牌都不可以
					table.cannot_chi_card(_seat_index, _send_card_data, table.GRR._cards_index[_seat_index]);
					table.cannot_chi_card(next_player, _send_card_data, table.GRR._cards_index[next_player]);

					table._current_player = next_player;
					_seat_index = next_player;
					table.log_error(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户");
					// 延时5秒发牌
					int dispatch_time = 3000;
					if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0
							&& sysParamModel1104.getVal5() < 10000) {
						dispatch_time = sysParamModel1104.getVal5();
					}
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
					table._last_card = _send_card_data;
					table._last_player = table._current_player;
					table.log_error(next_player + "发牌" + bAroseAction);
					table.log_error("_left_card_count:" + table.GRR._left_card_count);

				}
			}
		}

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
	public boolean handler_operate_card(YYWHZTable table, int seat_index, int operate_code, int operate_card,
			int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.log_error(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户" + seat_index + "操作用户");
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
		}
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
		if (operate_card != this._send_card_data) {
			table.log_player_error(seat_index, "DispatchCard 操作牌，与当前牌不一样");
			return true;
		}
		if (table.has_rule(GameConstants.GAME_RULE_YYWHZ_HU_FIRST) && seat_index == _seat_index
				&& table.has_rule(GameConstants.GAME_RULE_YYWHZ_KA_WAI)) {
			if (playerStatus.has_action_by_code(GameConstants.WIK_YYWHZ_WAI)
					&& operate_code != GameConstants.WIK_YYWHZ_WAI && operate_code != GameConstants.WIK_ZI_MO) {
				return true;
			}
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
				1);
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
						playerStatus.set_exe_pass(true);
						for (int index = 1; index < 3; index++) {

							if (table._logic.get_card_color(operate_card) == table._logic
									.get_card_color(operate_card + index)
									&& table._logic.get_card_value(operate_card + index) < 10) {
								if (table.GRR._cards_index[seat_index][table._logic
										.switch_to_card_index(operate_card + index)] == 0) {
									break;
								}
							} else {
								break;
							}
						}
						for (int index = 1; index < 3; index++) {

							if (table._logic.get_card_color(operate_card) == table._logic
									.get_card_color(operate_card - index)
									&& table._logic.get_card_value(operate_card - index) > 1) {
								if (table.GRR._cards_index[seat_index][table._logic
										.switch_to_card_index(operate_card - index)] == 0) {
									break;
								}
							} else {
								break;
							}
						}
						flag = true;
						break;
					}

					break;
				case GameConstants.WIK_PENG: {
					table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
					playerStatus.set_exe_pass(true);
				}
					break;
				case GameConstants.WIK_YYWHZ_WAI: {
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
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
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
			table.log_error("优先级最高的人还没操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
		// 判断可不可以吃的上家用户
		if (target_action == GameConstants.WIK_LEFT || target_action == GameConstants.WIK_CENTER
				|| target_action == GameConstants.WIK_RIGHT || target_action == GameConstants.WIK_EQS) {
			if (target_player != this._seat_index) {
				int last_player = _seat_index;
				boolean flag = false;
				for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {

					switch (table._playerStatus[last_player]._action[j]) {
					case GameConstants.WIK_LEFT:
					case GameConstants.WIK_CENTER:
					case GameConstants.WIK_RIGHT:
					case GameConstants.WIK_XXD:
					case GameConstants.WIK_DDX:
					case GameConstants.WIK_EQS:
						if (target_action == GameConstants.WIK_NULL) {
							continue;
						}
						table.cannot_chi_card(last_player, operate_card, table.GRR._cards_index[last_player]);
						if (flag == false)
							if (table._playerStatus[last_player].get_exe_pass() == true) {
								flag = true;
								table._playerStatus[last_player].set_exe_pass(false);
							}

						break;
					}
				}
			}

		}

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS;

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
						if (target_action == GameConstants.WIK_NULL && table._playerStatus[i].get_exe_pass() == true) {
							table.cannot_chi_card(i, operate_card, table.GRR._cards_index[i]);
						}

						break;
					case GameConstants.WIK_PENG:
						if (!((target_action == GameConstants.WIK_NULL)
								|| (target_action & eat_type) != GameConstants.WIK_NULL))
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
			if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_YYWHZ_LIU_WAI)
					|| table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_YYWHZ_LIU_NEI)) {
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();

				int next_player = _seat_index;
				table._current_player = next_player;
				table._last_player = next_player;

				PlayerStatus curPlayerStatus = table._playerStatus[next_player];
				curPlayerStatus.reset();
				if (table.is_can_out_card(_seat_index)) {
					curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
				}
			} else {
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();

				if (table._playerStatus[_seat_index].lock_huan_zhang()) {
					// 显示胡牌
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						int cards[] = new int[GameConstants.MAX_HH_COUNT];
						int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

						table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards,
								table.GRR._weave_items[i], table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

					}
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
							GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
				} else {

					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
					// 要出牌，但是没有牌出设置成相公 下家用户发牌
					int pai_count = 0;
					for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
						if (table.GRR._cards_index[_seat_index][i] < 3)
							pai_count += table.GRR._cards_index[_seat_index][i];
					}

					if (pai_count == 0) {
						table._is_xiang_gong[_seat_index] = true;
						table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
						int next_player = (_seat_index + table.getTablePlayerNumber() + 1)
								% table.getTablePlayerNumber();
						// 用户状态
						table._playerStatus[_seat_index].clean_action();
						table._playerStatus[_seat_index].clean_status();
						table._current_player = next_player;
						table._last_player = next_player;

						table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
						table.log_error(next_player + "可以胡，而不胡的情况  " + _seat_index);
						return true;
					}
					int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
					int ting_count = table._playerStatus[_seat_index]._hu_card_count;

					if (ting_count > 0) {
						table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
					} else {
						ting_cards[0] = 0;
						table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
					}
					table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data }, true, 0);
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

					table._current_player = next_player;
					_seat_index = next_player;
					table._last_player = next_player;
					// 没有人要就加入到牌堆
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 0);
					table._last_card = _send_card_data;
					table.log_error(next_player + "发牌" + _seat_index + "  " + next_player);
				}
			}

			return true;

		}
		case GameConstants.WIK_LEFT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			table.cannot_outcard(target_player, 1, cbRemoveCard, target_card, true);

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_RIGHT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
			table.cannot_outcard(target_player, 1, cbRemoveCard, target_card, true);

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_CENTER: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			table.cannot_outcard(target_player, 1, cbRemoveCard, target_card, true);

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_EQS:// 吃二七十
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card, target_card };
			table.cannot_outcard(target_player, 1, cbRemoveCard, target_card, true);

			int target_card_value = table._logic.get_card_value(target_card);
			switch (target_card_value) {
			case 2:
				cbRemoveCard[0] = target_card + 5;
				cbRemoveCard[1] = target_card + 8;
				break;
			case 7:
				cbRemoveCard[0] = target_card - 5;
				cbRemoveCard[1] = target_card + 3;
				break;
			case 10:
				cbRemoveCard[0] = target_card - 8;
				cbRemoveCard[1] = target_card - 3;
				break;

			default:
				break;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_PENG: // 碰牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card, target_card };
			table.cannot_outcard(target_player, 1, cbRemoveCard, target_card, true);
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_YYWHZ_WAI: // 歪操作
		{
			table.exe_wai(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			if (target_player == _seat_index) {
				table._mo_card_index[target_player][table._logic.switch_to_card_index(operate_card)]++;
			} else {
				table._chi_card_index[target_player][table._logic.switch_to_card_index(operate_card)]++;
			}
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_yywhz(target_player, _seat_index, operate_card, true);
			table.countChiHuTimes(target_player, true);
			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}

			if (!(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_WU_DUI_WHZ)).is_empty()
					|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_SHI_DUI_WHZ)).is_empty()
					|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_YI_DUI_WHZ)).is_empty()
					|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_JIU_DUI_WHZ)).is_empty()) {
				table._hu_weave_count[target_player] = 0;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_YYWHZ_LIU_WAI:// 溜牌
		{
			table._mo_card_index[target_player][table._logic.switch_to_card_index(target_card)]++;
			table.exe_liu(target_player, _seat_index, target_action, operate_card, GameConstants.CHI_PENG_TYPE_DISPATCH,
					0);
			// table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(operate_card)]=0;
			// if(target_action == GameConstants.WIK_YYWHZ_LIU_NEI){
			// // 组合扑克
			// int wIndex = table.GRR._weave_count[seat_index]++;
			// table.GRR._weave_items[seat_index][wIndex].public_card = 1;
			// table.GRR._weave_items[seat_index][wIndex].center_card =
			// operate_card;
			// table.GRR._weave_items[seat_index][wIndex].weave_kind =
			// operate_code;
			//
			// table.GRR._weave_items[seat_index][wIndex].provide_player =
			// _seat_index;
			// table.GRR._weave_items[seat_index][wIndex].hu_xi =
			// table._logic.get_weave_hu_xi_yywhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);
			// }else{
			// for(int i=0;i<table.GRR._weave_count[seat_index];i++){
			// if(table.GRR._weave_items[seat_index][i].center_card ==
			// operate_card
			// && table.GRR._weave_items[seat_index][i].weave_kind ==
			// GameConstants.WIK_YYWHZ_WAI){
			// table.GRR._weave_items[seat_index][i].public_card = 1;
			// table.GRR._weave_items[seat_index][i].center_card = operate_card;
			// table.GRR._weave_items[seat_index][i].weave_kind = operate_code;
			//
			// table.GRR._weave_items[seat_index][i].provide_player =
			// _seat_index;
			// table.GRR._weave_items[seat_index][i].hu_xi =
			// table._logic.get_weave_hu_xi_yywhz_yywzh(table.GRR._weave_items[_seat_index][i]);
			// }
			// }
			// }
			// table._mo_card_index[seat_index][table._logic.switch_to_card_index(operate_card)]++;
			// int _action = operate_code;
			// // 效果
			// table.operate_effect_action(seat_index,
			// GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action
			// }, 1,
			// GameConstants.INVALID_SEAT);
			//
			// int cards[]= new int[GameConstants.MAX_YYWHZ_COUNT];
			// int hand_card_count =
			// table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index],
			// cards);
			//
			// table.operate_player_cards(seat_index, hand_card_count, cards,
			// table.GRR._weave_count[seat_index],
			// table.GRR._weave_items[seat_index]);
			//
			// table.operate_player_get_card(_seat_index, 0, null,
			// GameConstants.INVALID_SEAT,false);
			// int next_player = (_seat_index + table.getTablePlayerNumber() +
			// 1) % table.getTablePlayerNumber();
			// table._current_player = next_player;
			// table._last_player = next_player;
			// table._playerStatus[_seat_index].clean_action();
			// table._playerStatus[_seat_index].clean_status();
			// table.exe_dispatch_card(next_player, GameConstants.WIK_NULL,
			// 1000);
			return true;
		}

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(YYWHZTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
