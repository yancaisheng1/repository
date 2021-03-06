package com.cai.domain;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.cai.common.util.Signature;
import com.cai.common.util.XMLParser;
import com.cai.core.SystemConfig;
import com.cai.service.PtAPIServiceImpl;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 统一下单 封装类
 */
@XStreamAlias("xml")
public class UnifiedOrderReqData {

	private final String appid;
	// 商户号
	private final String mch_id;
	private final String device_info;
	// 随机字符串
	private final String nonce_str;
	private String sign;
	// 商品描述
	private String body;
	// 商品详情 --非必填
	private String detail;
	// 附加数据 --非必填
	private String attach;
	// 商户订单号
	private String out_trade_no;
	// 货币类型 --非必填
	private String fee_type;
	// 总金额
	private final int total_fee;
	// 终端IP
	private String spbill_create_ip;
	private String time_start;
	private String time_expire;
	private String goods_tag;
	// 通知地址
	private String notify_url;
	// 交易类型
	private final String trade_type;
	private String product_id;
	private String limit_pay;
	private String openid;

	private UnifiedOrderReqData(UnifiedOrderReqDataBuilder builder) {
		this.appid = builder.appid;
		this.mch_id = builder.mch_id;
		this.device_info = builder.device_info;
		this.nonce_str = XMLParser.getRandomStringByLength(32);
		this.body = builder.body;
		this.detail = builder.detail;
		this.attach = builder.attach;
		this.out_trade_no = builder.out_trade_no;
		this.fee_type = builder.fee_type;
		this.total_fee = builder.total_fee;
		this.spbill_create_ip = builder.spbill_create_ip;
		this.time_start = builder.time_start;
		this.time_expire = builder.time_expire;
		this.goods_tag = builder.goods_tag;
		this.notify_url = builder.notify_url;
		this.trade_type = builder.trade_type;
		this.product_id = builder.product_id;
		this.limit_pay = builder.limit_pay;
		this.openid = builder.openid;
		this.sign = Signature.getSign(toMap(), builder.key);
	}

	public String getAppid() {
		return appid;
	}

	public String getMch_id() {
		return mch_id;
	}

	public String getDevice_info() {
		return device_info;
	}

	public String getNonce_str() {
		return nonce_str;
	}

	public String getSign() {
		return sign;
	}

	public String getBody() {
		return body;
	}

	public String getDetail() {
		return detail;
	}

	public String getAttach() {
		return attach;
	}

	public String getOut_trade_no() {
		return out_trade_no;
	}

	public String getFee_type() {
		return fee_type;
	}

	public int getTotal_fee() {
		return total_fee;
	}

	public String getSpbill_create_ip() {
		return spbill_create_ip;
	}

	public String getTime_start() {
		return time_start;
	}

	public String getTime_expire() {
		return time_expire;
	}

	public String getGoods_tag() {
		return goods_tag;
	}

	public String getNotify_url() {
		return notify_url;
	}

	public String getTrade_type() {
		return trade_type;
	}

	public String getProduct_id() {
		return product_id;
	}

	public String getLimit_pay() {
		return limit_pay;
	}

	public String getOpenid() {
		return openid;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			Object obj;
			try {
				obj = field.get(this);
				if (obj != null) {
					map.put(field.getName(), obj);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	public static class UnifiedOrderReqDataBuilder {
		private String appid;
		private String mch_id;
		private String device_info;
		private String body;
		private String detail;
		private String attach;
		private String out_trade_no;
		private String fee_type;
		private int total_fee;
		private String spbill_create_ip;
		private String time_start;
		private String time_expire;
		private String goods_tag;
		private String notify_url;
		private String trade_type;
		private String product_id;
		private String limit_pay;
		private String openid;
		private String key;

		public UnifiedOrderReqDataBuilder(String appid, String mch_id, String body, String out_trade_no,
				Integer total_fee, String spbill_create_ip, String notify_url, String trade_type, String key) {
			if (appid == null) {
				throw new IllegalArgumentException("传入参数appid不能为null");
			}
			if (mch_id == null) {
				throw new IllegalArgumentException("传入参数mch_id不能为null");
			}
			if (body == null) {
				throw new IllegalArgumentException("传入参数body不能为null");
			}
			if (out_trade_no == null) {
				throw new IllegalArgumentException("传入参数out_trade_no不能为null");
			}
			if (total_fee == null) {
				throw new IllegalArgumentException("传入参数total_fee不能为null");
			}
			if (spbill_create_ip == null) {
				throw new IllegalArgumentException("传入参数spbill_create_ip不能为null");
			}
			if (notify_url == null) {
				throw new IllegalArgumentException("传入参数notify_url不能为null");
			}
			if (trade_type == null) {
				throw new IllegalArgumentException("传入参数trade_type不能为null");
			}
			if (key == null) {
				throw new IllegalArgumentException("传入参数key不能为null");
			}
			this.appid = appid;
			this.mch_id = mch_id;
			this.body = body;
			this.out_trade_no = out_trade_no;
			this.total_fee = total_fee;
			this.spbill_create_ip = spbill_create_ip;
			this.notify_url = notify_url;
			this.trade_type = trade_type;
			this.key = key;
		}

		public UnifiedOrderReqDataBuilder setDevice_info(String device_info) {
			this.device_info = device_info;
			return this;
		}

		public UnifiedOrderReqDataBuilder setDetail(String detail) {
			this.detail = detail;
			return this;
		}

		public UnifiedOrderReqDataBuilder setAttach(String attach) {
			this.attach = attach;
			return this;
		}

		public UnifiedOrderReqDataBuilder setFee_type(String fee_type) {
			this.fee_type = fee_type;
			return this;
		}

		public UnifiedOrderReqDataBuilder setTime_start(String time_start) {
			this.time_start = time_start;
			return this;
		}

		public UnifiedOrderReqDataBuilder setTime_expire(String time_expire) {
			this.time_expire = time_expire;
			return this;
		}

		public UnifiedOrderReqDataBuilder setGoods_tag(String goods_tag) {
			this.goods_tag = goods_tag;
			return this;
		}

		public UnifiedOrderReqDataBuilder setProduct_id(String product_id) {
			this.product_id = product_id;
			return this;
		}

		public UnifiedOrderReqDataBuilder setLimit_pay(String limit_pay) {
			this.limit_pay = limit_pay;
			return this;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public UnifiedOrderReqDataBuilder setOpenid(String openid) {
			this.openid = openid;
			return this;
		}

		public UnifiedOrderReqData build() {

			if ("JSAPI".equals(this.trade_type) && this.openid == null) {
				throw new IllegalArgumentException("当传入trade_type为JSAPI时，openid为必填参数");
			}
			if ("NATIVE".equals(this.trade_type) && this.product_id == null) {
				throw new IllegalArgumentException("当传入trade_type为NATIVE时，product_id为必填参数");
			}
			return new UnifiedOrderReqData(this);
		}
	}

	@Override
	public String toString() {
		return "UnifiedOrderReqData [appid=" + appid + ", mch_id=" + mch_id + ", device_info=" + device_info
				+ ", nonce_str=" + nonce_str + ", sign=" + sign + ", body=" + body + ", detail=" + detail + ", attach="
				+ attach + ", out_trade_no=" + out_trade_no + ", fee_type=" + fee_type + ", total_fee=" + total_fee
				+ ", spbill_create_ip=" + spbill_create_ip + ", time_start=" + time_start + ", time_expire="
				+ time_expire + ", goods_tag=" + goods_tag + ", notify_url=" + notify_url + ", trade_type=" + trade_type
				+ ", product_id=" + product_id + ", limit_pay=" + limit_pay + ", openid=" + openid + "]";
	}

}
