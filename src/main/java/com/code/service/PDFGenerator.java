package com.code.service;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.code.Model.DataItem;
import com.code.Model.Message;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Component
public class PDFGenerator {

	public void generateFeedbackPDF(DataItem[] dataItems, String outputFileName) throws IOException, DocumentException {
		Document document = null;
		try {
			document = new Document(PageSize.A4);
			PdfWriter.getInstance(document, new FileOutputStream(outputFileName));
			document.open();

			// Add title
			Paragraph title = new Paragraph("Feedback Report",
					new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD));
			title.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(title);

			// Add feedback data table
			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			table.setSpacingBefore(20f);
			table.setSpacingAfter(20f);

			// Add table headers
			addTableHeader(table);

			// Add feedback data rows
			addFeedbackDataRows(table, dataItems);

			document.add(table);
		} finally {
			if (document != null) {
				document.close();
			}
		}
	}

	private void addTableHeader(PdfPTable table) {
		table.addCell("Mobile Number");
		table.addCell("Feedback Message");
	}

	private void addFeedbackDataRows(PdfPTable table, DataItem[] dataItems) {
	    Map<String, Message> latestFeedbackMessages = new HashMap<>();
	    
	    // we will start Find the latest feedback message for each mobile number
	    for (DataItem item : dataItems) {
	        List<Message> messageList = item.getMessageList();
	        for (int i = messageList.size() - 1; i >= 0; i--) {
	            Message message = messageList.get(i);
	            if ("please give your feedback ".equals(message.getMessageText())) {
	                String mobileNumber = item.getMobileNumber();
	                // we will Check if there is at least one message after the current one
	                if (i + 1 < messageList.size()) {
	                    latestFeedbackMessages.put(mobileNumber, messageList.get(i + 1));
	                }
	                break; // here we will Stop searching for feedback messages once the latest one is found
	            }
	        }

	        }
	    
	    
	    // Add the latest feedback message for each mobile number to the PDF table
	    for (Map.Entry<String, Message> entry : latestFeedbackMessages.entrySet()) {
	        String mobileNumber = entry.getKey();
	        Message feedbackMessage = entry.getValue();
	        table.addCell(mobileNumber);
	        table.addCell(feedbackMessage.getMessageText());
	        
	        
	    }
	    
	}


}
