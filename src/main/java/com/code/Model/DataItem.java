package com.code.Model;

import java.util.List;

public class DataItem {
	private String mobileNumber;
	private String lastMessage;
	private String lastMessageDateTimeUTC;
	private String feedbackMessage;
    private List<Message> messageList;
	private Integer flag;

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public String getLastMessageDateTimeUTC() {
		return lastMessageDateTimeUTC;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public String getFeedbackMessage() {
		return feedbackMessage;
	}

	public void setFeedbackMessage(String feedbackMessage) {
		this.feedbackMessage = feedbackMessage;
	}

	public List<Message> getMessageList() {
		return messageList;
	}

	public void setMessageList(List<Message> messageList) {
		this.messageList = messageList;
	}
}