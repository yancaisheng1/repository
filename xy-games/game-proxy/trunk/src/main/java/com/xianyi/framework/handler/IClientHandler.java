/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.xianyi.framework.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.EServerType;
import com.cai.common.util.PBUtil;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Parser;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol.CommonProto;
import protobuf.clazz.Protocol.Request;

/**
 * 处理客户端请求
 * 
 * @author wu_hc
 */
public abstract class IClientHandler<T extends GeneratedMessage> {

	/**
	 * 
	 */
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * T 字段的描述[###自动绑定，不要手动修改####]
	 */
	private FieldDescriptor fieldDescriptor;

	/**
	 * 消息去向[###自动绑定，不要手动修改####]
	 */
	private EServerType msgType;

	private Parser<? extends GeneratedMessage> parser;

	/**
	 * request为顶层包,message为具体包
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void doExecute(Object message, Request request, C2SSession session) throws Exception {
		this.execute((T) message, request, session);
	}

	/**
	 * request为顶层包,message为具体包
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	public void doExecute(CommonProto commProto, Request request, C2SSession session) throws Exception {
		this.execute(toObject(commProto), request, session);
	}

	@SuppressWarnings("unchecked")
	public T toObject(CommonProto commProto) {
		return (T) PBUtil.toObject(commProto.getByte(), parser, GeneratedMessage.class);
	}

	/**
	 * 
	 * @param message
	 * @param request
	 * @param session
	 * @throws Exception
	 */
	protected abstract void execute(T req, Request topRequest, C2SSession session) throws Exception;

	/**
	 * 返回描述
	 * 
	 * @return
	 */
	public FieldDescriptor getFieldDescriptor() {
		return fieldDescriptor;
	}

	public void setFieldDescriptor(FieldDescriptor fieldDescriptor) {
		this.fieldDescriptor = fieldDescriptor;
	}

	public void setParse(Parser<? extends GeneratedMessage> parser) {
		this.parser = parser;
	}

	public EServerType getMsgType() {
		return msgType;
	}

	public void setMsgType(EServerType msgType) {
		this.msgType = msgType;
	}

}
