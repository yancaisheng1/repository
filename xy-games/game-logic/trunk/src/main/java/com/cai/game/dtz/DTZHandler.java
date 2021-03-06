package com.cai.game.dtz;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class DTZHandler<T extends Table_DTZ> {
	public abstract void exe(T table);

	protected boolean handler_check_auto_behaviour(T table, int seat_index, int card_data) {
		if (!table.is_sys() || !table.checkTrutess(seat_index))
			return false;
		return false;
	}

	protected boolean handler_check_auto_behaviour_not_gold(T table, int seat_index, int card_data) {
		if (!table.istrustee[seat_index])
			return false;
		return false;
	}

	public boolean handler_be_set_trustee(T table, int seat_index) {
		handler_check_auto_behaviour(table, seat_index, GameConstants.INVALID_VALUE);
		return false;
	}

	public boolean handler_be_set_trustee_not_gold(T table, int seat_index) {
		handler_check_auto_behaviour_not_gold(table, seat_index, GameConstants.INVALID_VALUE);
		return false;
	}

	public boolean handler_player_ready(T table, int seat_index) {
		return table.handler_player_ready(seat_index, false);
	}

	public boolean handler_player_be_in_room(T table, int seat_index) {
		return true;

	}

	public boolean handler_player_out_card(T table, int seat_index, int card) {
		return false;
	}

	/***
	 * 用户操作
	 */
	public boolean handler_operate_card(T table, int seat_index, int operate_code, int operate_card) {
		return false;
	}

	public boolean handler_release_room(T table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(T table, Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}
}
