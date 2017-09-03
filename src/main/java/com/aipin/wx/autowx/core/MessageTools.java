package com.aipin.wx.autowx.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aipin.wx.autowx.beans.BaseMsg;
import com.aipin.wx.autowx.beans.RecommendInfo;
import com.aipin.wx.autowx.utils.Config;
import com.aipin.wx.autowx.utils.MyHttpClient;
import com.aipin.wx.autowx.utils.enums.StorageLoginInfoEnum;
import com.aipin.wx.autowx.utils.enums.URLEnum;
import com.aipin.wx.autowx.utils.enums.VerifyFriendEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


/**
 * 消息处理类
 * 
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午2:30:37
 * @version 1.0
 *
 */
public class MessageTools {
	private static Logger LOG = LoggerFactory.getLogger(MessageTools.class);
     Core core = null;
	  MyHttpClient myHttpClient  ;

	public MessageTools(Core core) {
		this.core=core;
		myHttpClient = core.getMyHttpClient();
	}
	/**
	 * 根据UserName发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:17:38
	 * @param msg
	 * @param toUserName
	 */
	private   void sendMsg(String text, String toUserName) {
		if (text == null) {
			return;
		}
		LOG.info(String.format("发送消息 %s: %s", toUserName, text));
		webWxSendMsg(1, text, toUserName);
	}

	/**
	 * 根据ID发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月6日 上午11:45:51
	 * @param text
	 * @param id
	 */
	public   void sendMsgById(String text, String id) {
		if (text == null) {
			return;
		}
		sendMsg(text, id);
	}

	/**
	 * 根据NickName发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:17:38
	 * @param text
	 * @param nickName
	 */
	public   boolean sendMsgByNickName(String text, String nickName) {
		if (nickName != null) {
			String toUserName = this.getUserNameByNickName(nickName);
			if (toUserName != null) {
				webWxSendMsg(1, text, toUserName);
				return true;
			}
		}
		return false;

	}

	/**
	 * 消息发送
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午2:32:02
	 * @param msgType
	 * @param content
	 * @param toUserName
	 */
	public   void webWxSendMsg(int msgType, String content, String toUserName) {
		String url = String.format(URLEnum.WEB_WX_SEND_MSG.getUrl(), core.getLoginInfo().get("url"));
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", msgType);
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getUserName());
		msgMap.put("ToUserName", toUserName == null ? core.getUserName() : toUserName);
		msgMap.put("LocalID", new Date().getTime() * 10);
		msgMap.put("ClientMsgId", new Date().getTime() * 10);
		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		try {
			String paramStr = JSON.toJSONString(paramMap);
			HttpEntity entity = myHttpClient.doPost(url, paramStr);
			EntityUtils.toString(entity, Consts.UTF_8);
		} catch (Exception e) {
			LOG.error("webWxSendMsg", e);
		}
	}

	/**
	 * 上传多媒体文件到 微信服务器，目前应该支持3种类型: 1. pic 直接显示，包含图片，表情 2.video 3.doc 显示为文件，包含PDF等
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 上午12:41:13
	 * @param filePath
	 * @return
	 */
	private   JSONObject webWxUploadMedia(String filePath) {
		File f = new File(filePath);
		if (!f.exists() && f.isFile()) {
			LOG.info("file is not exist");
			return null;
		}
		String url = String.format(URLEnum.WEB_WX_UPLOAD_MEDIA.getUrl(), core.getLoginInfo().get("fileUrl"));
		String mimeType = new MimetypesFileTypeMap().getContentType(f);
		String mediaType = "";
		if (mimeType == null) {
			mimeType = "text/plain";
		} else {
			mediaType = mimeType.split("/")[0].equals("image") ? "pic" : "doc";
		}
		String lastModifieDate = new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date());
		long fileSize = f.length();
		String passTicket = (String) core.getLoginInfo().get("pass_ticket");
		String clientMediaId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(0, 4);
		String webwxDataTicket = MyHttpClient.getCookie("webwx_data_ticket");
		if (webwxDataTicket == null) {
			LOG.error("get cookie webwx_data_ticket error");
			return null;
		}

		Map<String, Object> paramMap = core.getParamMap();

		paramMap.put("ClientMediaId", clientMediaId);
		paramMap.put("TotalLen", fileSize);
		paramMap.put("StartPos", 0);
		paramMap.put("DataLen", fileSize);
		paramMap.put("MediaType", 4);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		builder.addTextBody("id", "WU_FILE_0", ContentType.TEXT_PLAIN);
		builder.addTextBody("name", filePath, ContentType.TEXT_PLAIN);
		builder.addTextBody("type", mimeType, ContentType.TEXT_PLAIN);
		builder.addTextBody("lastModifieDate", lastModifieDate, ContentType.TEXT_PLAIN);
		builder.addTextBody("size", String.valueOf(fileSize), ContentType.TEXT_PLAIN);
		builder.addTextBody("mediatype", mediaType, ContentType.TEXT_PLAIN);
		builder.addTextBody("uploadmediarequest", JSON.toJSONString(paramMap), ContentType.TEXT_PLAIN);
		builder.addTextBody("webwx_data_ticket", webwxDataTicket, ContentType.TEXT_PLAIN);
		builder.addTextBody("pass_ticket", passTicket, ContentType.TEXT_PLAIN);
		builder.addBinaryBody("filename", f, ContentType.create(mimeType), filePath);
		HttpEntity reqEntity = builder.build();
		HttpEntity entity = myHttpClient.doPostFile(url, reqEntity);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result);
			} catch (Exception e) {
				LOG.error("webWxUploadMedia 错误： ", e);
			}

		}
		return null;
	}

	/**
	 * 根据NickName发送图片消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:32:45
	 * @param nackName
	 * @return
	 */
	public   boolean sendPicMsgByNickName(String nickName, String filePath) {
		String toUserName = this.getUserNameByNickName(nickName);
		if (toUserName != null) {
			return sendPicMsgByUserId(toUserName, filePath);
		}
		return false;
	}

	/**
	 * 根据用户id发送图片消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:34:24
	 * @param nickName
	 * @param filePath
	 * @return
	 */
	public   boolean sendPicMsgByUserId(String userId, String filePath) {
		JSONObject responseObj = webWxUploadMedia(filePath);
		if (responseObj != null) {
			String mediaId = responseObj.getString("MediaId");
			if (mediaId != null) {
				return webWxSendMsgImg(userId, mediaId);
			}
		}
		return false;
	}

	/**
	 * 发送图片消息，内部调用
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:38:55
	 * @return
	 */
	private  boolean webWxSendMsgImg(String userId, String mediaId) {
		String url = String.format("%s/webwxsendmsgimg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", 3);
		msgMap.put("MediaId", mediaId);
		msgMap.put("FromUserName", core.getUserSelf().getString("UserName"));
		msgMap.put("ToUserName", userId);
		String clientMsgId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("BaseRequest", core.getParamMap().get("BaseRequest"));
		paramMap.put("Msg", msgMap);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = myHttpClient.doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				LOG.error("webWxSendMsgImg 错误： ", e);
			}
		}
		return false;

	}

	/**
	 * 根据用户id发送文件
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午11:57:36
	 * @param userId
	 * @param filePath
	 * @return
	 */
	public  boolean sendFileMsgByUserId(String userId, String filePath) {
		String title = new File(filePath).getName();
		Map<String, String> data = new HashMap<String, String>();
		data.put("appid", Config.API_WXAPPID);
		data.put("title", title);
		data.put("totallen", "");
		data.put("attachid", "");
		data.put("type", "6"); // APPMSGTYPE_ATTACH
		data.put("fileext", title.split("\\.")[1]); // 文件后缀
		JSONObject responseObj = webWxUploadMedia(filePath);
		if (responseObj != null) {
			data.put("totallen", responseObj.getString("StartPos"));
			data.put("attachid", responseObj.getString("MediaId"));
		} else {
			LOG.error("sednFileMsgByUserId 错误: ", data);
		}
		return webWxSendAppMsg(userId, data);
	}

	/**
	 * 根据用户昵称发送文件消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月10日 下午10:59:27
	 * @param nickName
	 * @param filePath
	 * @return
	 */
	public  boolean sendFileMsgByNickName(String nickName, String filePath) {
		String toUserName = this.getUserNameByNickName(nickName);
		if (toUserName != null) {
			return sendFileMsgByUserId(toUserName, filePath);
		}
		return false;
	}

	/**
	 * 内部调用
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月10日 上午12:21:28
	 * @param userId
	 * @param data
	 * @return
	 */
	private  boolean webWxSendAppMsg(String userId, Map<String, String> data) {
		String url = String.format("%s/webwxsendappmsg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		String clientMsgId = String.valueOf(new Date().getTime())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		String content = "<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''><title>" + data.get("title")
				+ "</title><des></des><action></action><type>6</type><content></content><url></url><lowurl></lowurl>"
				+ "<appattach><totallen>" + data.get("totallen") + "</totallen><attachid>" + data.get("attachid")
				+ "</attachid><fileext>" + data.get("fileext") + "</fileext></appattach><extinfo></extinfo></appmsg>";
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("Type", data.get("type"));
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getUserSelf().getString("UserName"));
		msgMap.put("ToUserName", userId);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		/*
		 * Map<String, Object> paramMap = new HashMap<String, Object>();
		 * 
		 * @SuppressWarnings("unchecked") Map<String, Map<String, String>>
		 * baseRequestMap = (Map<String, Map<String, String>>)
		 * core.getLoginInfo() .get("baseRequest"); paramMap.put("BaseRequest",
		 * baseRequestMap.get("BaseRequest"));
		 */

		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = myHttpClient.doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				LOG.error("错误: ", e);
			}
		}
		return false;
	}

	/**
	 * 被动添加好友
	 * 
	 * @date 2017年6月29日 下午10:08:43
	 * @param msg
	 * @param accept
	 *            true 接受 false 拒绝
	 */
	public  void addFriend(BaseMsg msg, boolean accept) {
		if (!accept) { // 不添加
			return;
		}
		int status = VerifyFriendEnum.ACCEPT.getCode(); // 接受好友请求
		RecommendInfo recommendInfo = msg.getRecommendInfo();
		String userName = recommendInfo.getUserName();
		String ticket = recommendInfo.getTicket();
		// 更新好友列表
		// TODO 此处需要更新好友列表
		// core.getContactList().add(msg.getJSONObject("RecommendInfo"));

		String url = String.format(URLEnum.WEB_WX_VERIFYUSER.getUrl(), core.getLoginInfo().get("url"),
				String.valueOf(System.currentTimeMillis() / 3158L), core.getLoginInfo().get("pass_ticket"));

		List<Map<String, Object>> verifyUserList = new ArrayList<Map<String, Object>>();
		Map<String, Object> verifyUser = new HashMap<String, Object>();
		verifyUser.put("Value", userName);
		verifyUser.put("VerifyUserTicket", ticket);
		verifyUserList.add(verifyUser);

		List<Integer> sceneList = new ArrayList<Integer>();
		sceneList.add(33);

		JSONObject body = new JSONObject();
		body.put("BaseRequest", core.getParamMap().get("BaseRequest"));
		body.put("Opcode", status);
		body.put("VerifyUserListSize", 1);
		body.put("VerifyUserList", verifyUserList);
		body.put("VerifyContent", "");
		body.put("SceneListCount", 1);
		body.put("SceneList", sceneList);
		body.put("skey", core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey()));

		String result = null;
		try {
			String paramStr = JSON.toJSONString(body);
			HttpEntity entity = myHttpClient.doPost(url, paramStr);
			result = EntityUtils.toString(entity, Consts.UTF_8);
		} catch (Exception e) {
			LOG.error("webWxSendMsg", e);
		}

		if (StringUtils.isBlank(result)) {
			LOG.error("被动添加好友失败");
		}

		LOG.debug(result);

	}

	/**
	 * 根据用户名发送文本消息
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午10:43:14
	 * @param msg
	 * @param toUserName
	 */
	public   void sendMsgByUserName(String msg, String toUserName) {
		this.sendMsgById(msg, toUserName);
	}

	/**
	 * <p>
	 * 通过RealName获取本次UserName
	 * </p>
	 * <p>
	 * 如NickName为"yaphone"，则获取UserName=
	 * "@1212d3356aea8285e5bbe7b91229936bc183780a8ffa469f2d638bf0d2e4fc63"，
	 * 可通过UserName发送消息
	 * </p>
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午10:56:31
	 * @param name
	 * @return
	 */
	public   String getUserNameByNickName(String nickName) {
		for (JSONObject o : core.getContactList()) {
			if (o.getString("NickName").equals(nickName)) {
				return o.getString("UserName");
			}
		}
		return null;
	}

	/**
	 * 返回好友昵称列表
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:37:20
	 * @return
	 */
	public   List<String> getContactNickNameList() {
		List<String> contactNickNameList = new ArrayList<String>();
		for (JSONObject o : core.getContactList()) {
			contactNickNameList.add(o.getString("NickName"));
		}
		return contactNickNameList;
	}

	/**
	 * 返回好友完整信息列表
	 * 
	 * @date 2017年6月26日 下午9:45:39
	 * @return
	 */
	public  List<JSONObject> getContactList() {
		return core.getContactList();
	}

	/**
	 * 返回群列表
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月5日 下午9:55:21
	 * @return
	 */
	public  List<JSONObject> getGroupList() {
		return core.getGroupList();
	}

	/**
	 * 获取群ID列表
	 * 
	 * @date 2017年6月21日 下午11:42:56
	 * @return
	 */
	public  List<String> getGroupIdList() {
		return core.getGroupIdList();
	}

	/**
	 * 获取群NickName列表
	 * 
	 * @date 2017年6月21日 下午11:43:38
	 * @return
	 */
	public  List<String> getGroupNickNameList() {
		return core.getGroupNickNameList();
	}

	/**
	 * 根据groupIdList返回群成员列表
	 * 
	 * @date 2017年6月13日 下午11:12:31
	 * @param groupId
	 * @return
	 */
	public  JSONArray getMemberListByGroupId(String groupId) {
		return core.getGroupMemeberMap().get(groupId);
	}

	/**
	 * 退出微信
	 * 
	 * @author https://github.com/yaphone
	 * @date 2017年5月18日 下午11:56:54
	 */
	public  void logout() {
		webWxLogout();
	}

	private  boolean webWxLogout() {
		String url = String.format(URLEnum.WEB_WX_LOGOUT.getUrl(),
				core.getLoginInfo().get(StorageLoginInfoEnum.url.getKey()));
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("redirect", "1"));
		params.add(new BasicNameValuePair("type", "1"));
		params.add(
				new BasicNameValuePair("skey", (String) core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey())));
		try {
			HttpEntity entity = core.getMyHttpClient().doGet(url, params, false, null);
			String text = EntityUtils.toString(entity, Consts.UTF_8); // 无消息
			return true;
		} catch (Exception e) {
			LOG.debug(e.getMessage());
		}
		return false;
	}

	public  void setUserInfo() {
		for (JSONObject o : core.getContactList()) {
			core.getUserInfoMap().put(o.getString("NickName"), o);
			core.getUserInfoMap().put(o.getString("UserName"), o);
		}
	}

	/**
	 * 
	 * 根据用户昵称设置备注名称
	 * 
	 * @date 2017年5月27日 上午12:21:40
	 * @param userName
	 * @param remName
	 */
	public  void remarkNameByNickName(String nickName, String remName) {
		String url = String.format(URLEnum.WEB_WX_REMARKNAME.getUrl(), core.getLoginInfo().get("url"),
				core.getLoginInfo().get(StorageLoginInfoEnum.pass_ticket.getKey()));
		Map<String, Object> msgMap = new HashMap<String, Object>();
		Map<String, Object> msgMap_BaseRequest = new HashMap<String, Object>();
		msgMap.put("CmdId", 2);
		msgMap.put("RemarkName", remName);
		msgMap.put("UserName", core.getUserInfoMap().get(nickName).get("UserName"));
		msgMap_BaseRequest.put("Uin", core.getLoginInfo().get(StorageLoginInfoEnum.wxuin.getKey()));
		msgMap_BaseRequest.put("Sid", core.getLoginInfo().get(StorageLoginInfoEnum.wxsid.getKey()));
		msgMap_BaseRequest.put("Skey", core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey()));
		msgMap_BaseRequest.put("DeviceID", core.getLoginInfo().get(StorageLoginInfoEnum.deviceid.getKey()));
		msgMap.put("BaseRequest", msgMap_BaseRequest);
		try {
			String paramStr = JSON.toJSONString(msgMap);
			HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
			// String result = EntityUtils.toString(entity, Consts.UTF_8);
			LOG.info("修改备注" + remName);
		} catch (Exception e) {
			LOG.error("remarkNameByUserName", e);
		}
	}

	/**
	 * 获取微信在线状态
	 * 
	 * @date 2017年6月16日 上午12:47:46
	 * @return
	 */
	public  boolean getWechatStatus() {
		return core.isAlive();
	}
}
