package com.cai.future.runnable;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.domain.Room;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.BaseFuture;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

public class CreateTimeOutRunnable extends BaseFuture {

	private static Logger logger = LoggerFactory.getLogger(CreateTimeOutRunnable.class);
	
	private int _room_id;
	
	public CreateTimeOutRunnable(int room_id){
		super(room_id);
		_room_id = room_id;
	}
	
	
	@Override
	public void execute() {
		try {
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(_room_id);
			if(table==null){
				return ;
			}
			ReentrantLock roomLock = table.getRoomLock();
			try{
				roomLock.lock();
				if(PlayerServiceImpl.getInstance().getRoomMap().containsKey(_room_id)) {
					table.runnable_create_time_out();
				}
			}finally{
				roomLock.unlock();
				
			}
			
		} catch (Exception e) {
			logger.error("error"+_room_id,e);
			Room room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if(room!=null) {
				MongoDBServiceImpl.getInstance().server_error_log(room.getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(e),
						0L, SysGameTypeDict.getInstance().getGameDescByTypeIndex(room.getGameTypeIndex()), room.getGame_id());
			}
		}
	}

}
