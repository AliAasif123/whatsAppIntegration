package com.code.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
	private String messageId;
	private String waMessageId;
	private String messageText;
	private String isInBound;
	private int messageType;
	private String sourceURL;
	private int isReply;
	private String referenceWAMessageId;
	private String messageDateTime;
	private String caption;
	private String messageFormat;
	private String websiteURL;
	private int waDeliveryStatus;
}
