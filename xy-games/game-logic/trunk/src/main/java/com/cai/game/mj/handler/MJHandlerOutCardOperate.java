package com.cai.game.mj.handler;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerOutCardOperate<T extends AbstractMJTable> extends AbstractMJHandler<T> {

    public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
    public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
    public int _type;

    public boolean can_rm_liang_zhang; // 商丘麻将胡出牌移除亮张

    protected int _current_player = GameConstants.INVALID_SEAT;

    public void reset_status(int seat_index, int card, int type) {
        _out_card_player = seat_index;
        _out_card_data = card;
        _type = type;
    }

    public void reset_status(int seat_index, int card, int type, boolean is_liang_zhang) {
        reset_status(seat_index, card, type);
        can_rm_liang_zhang = is_liang_zhang;
    }

    public void reset_status() {

    }

    @Override
    public void exe(T table) {
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
        tableResponse.setCurrentPlayer(_out_card_player);
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
            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
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

        // 出牌
        table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
                seat_index);

        // table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
        if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
            table.operate_player_action(seat_index, false);
        }

        return true;
    }

}