package com.cai.game.phz.handler.nxghz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.phz.handler.PHZHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class GHZHandlerGang_NX extends PHZHandlerGang<NXGHZTable> {

	@Override
	public void exe(NXGHZTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		
		if(_depatch == false)
			table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);
		else
			table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT,false);
	

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 5,
				GameConstants.INVALID_SEAT);
		
		this.exe_gang(table);


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
	public boolean handler_operate_card(NXGHZTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		// 抢杠胡

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_ZI_MO))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,操作牌对象出错");
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

//		if (operate_code == GameConstants.WIK_NULL) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
//			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
//		} else if (operate_code == GameConstants.WIK_CHI_HU) {
//			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
//			table.process_chi_hu_player_operate_hh(seat_index, new int[] { _center_card }, 1, false);// 效果
//		} else {
//			table.log_player_error(seat_index, "HHHandlerGang_YX出牌操作,没有动作");
//			return false;
//		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
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
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			table.log_error("最高用户操作"+target_player);
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL:
		{
			
			if((table._is_xiang_gong[_seat_index] == false )&&(table._long_count[_seat_index] == 1||GameConstants.SAO_TYPE_MINE_SAO  == _type))
			{    
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				int pai_count =0;
				for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
			       	if(table.GRR._cards_index[_seat_index][i]<3)
			       		pai_count += table.GRR._cards_index[_seat_index][i];
				 }
				if(pai_count == 0)
				{
					table._is_xiang_gong[_seat_index] = true;	 	
		        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
		        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;
					
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					table.log_error(next_player + "提 扫 跑 发牌"+_seat_index);
					
		    	
				}
				else{  //胡牌了不执行
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
					//table.log_player_error(_seat_index, "扫和提龙出牌状态");
				}
				
			}
			else{
				table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
						table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;
				
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1500);
				table.log_error(next_player + "提 扫 跑 发牌"+_seat_index);
				
			}


		
		    
			
			return true;

		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = target_player;
//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
			if(_depatch == true)
				table.operate_player_get_card(this._provide_player, 1, new int[] { _center_card }, GameConstants.INVALID_SEAT,false);
			
			table._shang_zhuang_player = _seat_index;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_wmq(_seat_index, _provide_player, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[_seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[_seat_index]++;
			}
			table.countChiHuTimes_wmq(_seat_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[_seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[_seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}



		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	protected boolean exe_gang(NXGHZTable table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;
		boolean is_ting_hu = true; 
		if (GameConstants.PAO_TYPE_AN_LONG == _type) {
			// 暗龙
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table._long_count[_seat_index]++;

		}else if(GameConstants.PAO_TYPE_TI_MINE_LONG== _type){
			// 提龙
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table._long_count[_seat_index]++;
		}else if(GameConstants.PAO_TYPE_MINE_SAO_LONG== _type){
			// 提龙
			// 设置变量
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if(cbWeaveKind != GameConstants.WIK_AN_LONG)
					is_ting_hu = false;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_WEI)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}else if (GameConstants.PAO_TYPE_OTHER_SAO_PAO == _type) {
			// 别人打的牌
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_WEI)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					if(table.GRR._cards_index[_seat_index][cbCardIndex] == 3)
						table.cannot_outcard(_seat_index, -1, _center_card, true);
					break;
				}
			}
			
			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
			//table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
			
		

		} 
		else if (GameConstants.PAO_TYPE_OHTER_PAO  == _type) {
			// 别人打的牌
			table._long_count[_seat_index]++;
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;			
		
			//table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		}else if (GameConstants.PAO_TYPE_MINE_PENG_PAO  == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			} 
		
			

		}else if (GameConstants.SAO_TYPE_MINE_SAO  == _type){
			//扫牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		}
		
		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi_yywhz_nxghz(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
		int cbMingIndexTemp[] = new int [GameConstants.MAX_HH_INDEX];
		table._logic.ming_index_temp(cbMingIndexTemp,table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],false,0);
		table._hu_xi[_seat_index] = table._logic.get_all_hu_xi_weave(table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index], cbMingIndexTemp);
	
	
		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] -=2 ;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		int hu_xi_count = table._logic.get_weave_hu_xi_yywhz_nxghz(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);
		
		int pai_count =0;
		int action_hu = GameConstants.WIK_NULL;
		for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
	       	if(table.GRR._cards_index[_seat_index][i]<3)
	       		pai_count += table.GRR._cards_index[_seat_index][i];
		 }
		if((_depatch == true)&&(table._ti_two_long[_seat_index] == false)&&(table._is_xiang_gong[_seat_index] == false)){
			// 变量定义
			ChiHuRight chr = new ChiHuRight();
		
			chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			
	
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			int hu_xi[] = new int[1];
			action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index],_seat_index,_provide_player,0, chr, card_type,hu_xi,true);// 自摸
			
			if(action_hu != GameConstants.WIK_NULL){
				PlayerStatus  tempPlayerStatus = table._playerStatus[_seat_index];
				tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_center_card, _seat_index);
				if(table.has_rule(GameConstants.GAME_RULE_QIANG_HU_PAI))
				{
					table.handler_operate_card(_seat_index, GameConstants.WIK_ZI_MO,_center_card, -1);
					return true;
				}
				else{
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(_center_card, _seat_index);
				}
				 if (tempPlayerStatus.has_action()) {
					 tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
					 if(hu_xi[_seat_index] == 10)
						{
							chr.opr_or(GameConstants.CHR_KA_WEI_WMQ);
						}
					 //操作状态
					  table.operate_player_action(_seat_index, false);
					  return true;
					 }
			}
			else{
				chr.set_empty();
			}
	       
	       
	        if(pai_count == 0)
	        {
	        	int all_hu_xi = 0;
	        	for(int i = 0; i<table.GRR._weave_count[_seat_index];i++ )
	        	{
	        		all_hu_xi += table.GRR._weave_items[_seat_index][i].hu_xi;
	        	}
	        	if(all_hu_xi>=15)
	        	{
	        		int hong_pai_count = 0;
					int hei_pai_count = 0;
					int all_cards_count = 0;
					int hang_hang_xi_count = 0;
					
	        		for(int i = 0; i<table.GRR._weave_count[_seat_index];i++ )
	            	{
	            		table._hu_weave_items[_seat_index][i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
	            		table._hu_weave_items[_seat_index][i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
	            		table._hu_weave_items[_seat_index][i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
	            		table._hu_weave_items[_seat_index][i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
	            		table._hu_weave_items[_seat_index][i].hu_xi = table.GRR._weave_items[_seat_index][i].hu_xi;
	            		if(table._hu_weave_items[_seat_index][i].hu_xi > 0)
	            			hang_hang_xi_count ++;
	            		hong_pai_count += table._logic.calculate_weave_hong_pai(table._hu_weave_items[_seat_index][i]);
	            		hei_pai_count += table._logic.calculate_weave_hei_pai(table._hu_weave_items[_seat_index][i]);
	            		
	            		
	            		
	            	}
	        		table._hu_weave_count[_seat_index] = table.GRR._weave_count[_seat_index];
	        		all_cards_count = hong_pai_count +hei_pai_count;
	        		
					if ((card_type == GameConstants.HU_CARD_TYPE_ZIMO)&&(_seat_index == _provide_player)) {
						chr.opr_or(GameConstants.CHR_ZI_MO);
					}
					else if ((card_type == GameConstants.HU_CARD_TYPE_ZIMO)&&(_seat_index != _provide_player)) {
						chr.opr_or(GameConstants.CHR_JEI_PAO_HU);
					}
					if(hong_pai_count >= 10)
					{
						table._hong_pai_count[_seat_index] = hong_pai_count;
						chr.opr_or(GameConstants.CHR_TEN_HONG_PAI);
					}
					if(hong_pai_count == 1)
					{
						chr.opr_or(GameConstants.CHR_ONE_HONG);
					}
					if(hei_pai_count == 1)
					{
						chr.opr_or(GameConstants.CHR_ONE_HEI);
					}
					if(hei_pai_count == all_cards_count)
					{
						chr.opr_or(GameConstants.CHR_ALL_HEI);
					}
					if(table.GRR._left_card_count == 0)
					{
						chr.opr_or(GameConstants.CHR_HAI_HU);
					}
					if(table._da_pai_count[_seat_index] >=18){
						chr.opr_or(GameConstants.CHR_DA_HU);
					}
					if(table._xiao_pai_count[_seat_index] >=16){
						chr.opr_or(GameConstants.CHR_XIAO_HU);
					}
					
					if(table.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)){
						if(table._huang_zhang_count >= 1){
							chr.opr_or(GameConstants.CHR_HUANG_FAN);
						}
					}
	        		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
	        		curPlayerStatus.reset();

					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(_center_card, _seat_index);
					if(table.has_rule(GameConstants.GAME_RULE_QIANG_HU_PAI))
					{
						table.handler_operate_card(_seat_index, GameConstants.WIK_ZI_MO,_center_card, -1);
						return true;
					}
					else{
						curPlayerStatus.add_action(GameConstants.WIK_NULL);
						curPlayerStatus.add_pass(_center_card, _seat_index);
					}	
					 if (curPlayerStatus.has_action()) {
						  curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
						 //操作状态
						  table.operate_player_action(_seat_index, false);
						  return true;
						 }
	        	}
	        	else{
					chr.set_empty();
				}	        	
	        }
		
		}
      	if(table.is_can_out_card(_seat_index)== false)
      		table._is_xiang_gong[_seat_index] = true;

		if((table._is_xiang_gong[_seat_index] == false )&&(table._long_count[_seat_index] == 1||GameConstants.SAO_TYPE_MINE_SAO  == _type))
		{    
			//要出牌，但是没有牌出设置成相公  下家用户发牌
			if(pai_count == 0)
			{
				table._is_xiang_gong[_seat_index] = true;	 	
	        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
	        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				table._last_player = next_player;
				
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
	    	
			}
			else{
				if(table._ti_two_long[_seat_index] == false)
				{
					//胡牌了不执行
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
					//table.log_player_error(_seat_index, "扫和提龙出牌状态");
				}
				
			}
			
		}
		else{
			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
		}

		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(NXGHZTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

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
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);
		table.istrustee[seat_index]=false;
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_HUN);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(seat_index!=i) {
					if((table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_GANG || table.GRR._weave_items[i][j].weave_kind==GameConstants.WIK_ZHAO) &&table.GRR._weave_items[i][j].public_card==0) {
						weaveItem_item.setCenterCard(0);
					}else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addHuXi(table._hu_xi[i]);
			//
			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for(int j=0; j < hand_card_count; j++){
			if( table._logic.is_magic_card(cards[j])){
				cards[j]+=GameConstants.CARD_ESPECIAL_TYPE_HUN;//将癞子转下
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		table.operate_cannot_card(seat_index);
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,seat_index);
		
		
		if(table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone()==false){
			table.operate_player_action(seat_index, false);
		}
		
		if(table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index,table._is_xiang_gong[seat_index]);
		
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
