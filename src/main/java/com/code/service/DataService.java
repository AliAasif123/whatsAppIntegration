package com.code.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.code.Model.DataItem;
import com.code.Model.Response;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataService {
	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private PDFGenerator pdfGenerator;

	@Autowired
	private FTPUploader ftpUploader;

//	public static String getDataFromURLs(String urlString) throws IOException {
//		URL url = new URL(urlString);
//		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//		conn.setRequestMethod("GET");
//
//		int responseCode = conn.getResponseCode();
//		if (responseCode == HttpURLConnection.HTTP_OK) {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//			StringBuilder response = new StringBuilder();
//			String line;
//			while ((line = reader.readLine()) != null) {
//				response.append(line);
//			}
//			reader.close();
//			return response.toString();
//		} else {
//			System.out.println("Error: Failed to fetch data from URL. Response code: " + responseCode);
//			return null;
//		}
//
//	}

	private static final String FACEBOOK_GRAPH_API_URL = "https://graph.facebook.com/v19.0/200494486471920/messages";
	private static final String ACCESS_TOKEN = "EAALk7dAjlH0BO4ZBIOUGpZA6Dey8JaD2ZAyj4tSF8irEZAofInllYectN6F0rdsxAjzPy2IikTYhm57LZC5ZAYTswmQAPavIcWaPmlAoOo9XaM2Q3kdw7cTO7rqQT0cfNZAVQXxKUt5rvUlqalunxuxK26pZCV7OyekSD5ew8Ydn7A7RNsChFfMyBjZBZBvI7ZCU7yF";
	private static final String RECIPIENT_NUMBER = "9182650986";
	private static final String MESSAGE = "*Shan Baghs*:Thank you for visiting our restaurant! We value your feedback. Please rate your experience between 1 and 5.";
	private final RestTemplate restTemplate;

	public DataService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	private Map<String, LocalDateTime> lastSentTimestamps = new HashMap<>();

	// @Scheduled(cron = "*/1 * * * * *")
	public void sendMessage() {
		try {
			List<String> mobileNumbers = getUniqueMobileNumbersFromDatabase();
			for (String mobileNumber : mobileNumbers) {
				LocalDateTime lastSentTime = lastSentTimestamps.getOrDefault(mobileNumber, null);
				boolean flagForNumberFromDatabase = getFlagForNumberFromDatabase(mobileNumber);
				if (lastSentTime == null || lastSentTime.isBefore(LocalDateTime.now().minusHours(24))) {
					if (flagForNumberFromDatabase) {
						sendFeedbackMessage(mobileNumber);
						updateFlagForNumberInDatabase(mobileNumber, 1);
						lastSentTimestamps.put(mobileNumber, LocalDateTime.now());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean getFlagForNumberFromDatabase(String mobileNumber) {
		Query query = new Query(Criteria.where("Mobile Number").is(mobileNumber).and("flag").is(0));
		Document doc = mongoTemplate.findOne(query, Document.class, "Sceduled_data");
		return doc != null;
	}

	private void updateFlagForNumberInDatabase(String mobileNumber, int flag) {
		Query query = new Query(Criteria.where("Mobile Number").is(mobileNumber));
		Update update = new Update().set("flag", flag);
		mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Document.class,
				"Sceduled_data");
	}

	private List<String> getUniqueMobileNumbersFromDatabase() {
		List<String> mobileNumbers = new ArrayList<>();
		MongoCollection<Document> collection = mongoTemplate.getCollection("Sceduled_data");
		FindIterable<Document> documents = collection.find();
		for (Document doc : documents) {
			String mobileNumber = doc.getString("Mobile Number");
			mobileNumbers.add(mobileNumber);
		}
		return mobileNumbers;
	}

	public void sendFeedbackMessage(String recipientNumber) {
		try {
			String requestBody = "{ \"messaging_product\": \"whatsapp\", \"recipient_type\": \"individual\", \"to\": \""
					+ recipientNumber + "\", \"type\": \"text\", \"text\": { \"preview_url\": false, \"body\": \""
					+ MESSAGE + "\" } }";

//			String requestBody = "{ \"messaging_product\": \"whatsapp\", \"recipient_type\": \"individual\", \"to\": \""
//					+ recipientNumber
//					+ "\", \"type\": \"template\", \"template\": { \"name\": \"feedback_final\", \"language\": { \"code\": \"en\" } } }";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(ACCESS_TOKEN);
			HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(FACEBOOK_GRAPH_API_URL, request, String.class);
			System.out.println("Response status: " + response.getStatusCode());
			System.out.println("Response body: " + response.getBody());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Document> getDataFromURL(String urlString) {
		List<Document> documents = new ArrayList<>();
		try {
			RestTemplate restTemplate = new RestTemplate();
			Response responseData = restTemplate.getForObject(urlString, Response.class);
			if (responseData != null) {
				for (DataItem item : responseData.getData()) {

					// need to change lastMessage with actual....
					if (item.getLastMessage() != null && item.getLastMessage().contains("najeeb")
							&& item.getLastMessageDateTimeUTC() != null) {
						// here we will Check if entry exists in the database
						Document existingDocument = mongoTemplate.findOne(
								Query.query(Criteria.where("Mobile Number").is(item.getMobileNumber())), Document.class,
								"Sceduled_data");

						if (existingDocument == null) {
							// here If entry doesn't exist, we will add new entry
							Document newDocument = new Document();
							newDocument.append("Mobile Number", item.getMobileNumber())
									.append("Last Message Date and Time", item.getLastMessageDateTimeUTC())
									.append("flag", 0);

							documents.add(newDocument);
						} else {
							String dateString = existingDocument.getString("Last Message Date and Time");
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
							LocalDateTime lastMessageDateTimeDB = LocalDateTime.parse(dateString, formatter);
							LocalDateTime currentDateTime = LocalDateTime.now();
							long hoursDifference = ChronoUnit.HOURS.between(lastMessageDateTimeDB, currentDateTime);
							if (hoursDifference > 24) {
								// here If more than 24 hours, we will update entry
								LocalDateTime updatedDateTime = LocalDateTime.of(LocalDate.now(),
										lastMessageDateTimeDB.toLocalTime());
								existingDocument.put("Last Message Date and Time", updatedDateTime);
								existingDocument.put("flag", 0);
								documents.add(existingDocument);
							} else {
								// here If less than 24 hours
								existingDocument.put("Duplicate", true);
								log.warn("Duplicate entry found for mobile number: {}", item.getMobileNumber());

							}
						}
					}
				}
			} else {
				log.error("Failed to retrieve data from URL: {}", urlString);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Failed to retrieve data from URL: {}", urlString, e);
		}
		return documents;
	}

	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	// @Scheduled(cron = "0 */2 * * * *")
	// here little bit confusion, need to talk with anna about flag

	public void checkAndUpdateFlag() {
		try {
			List<String> mobileNumbers = getMobileNumbersWithFlagOne();
			for (String mobileNumber : mobileNumbers) {
				LocalDateTime lastMessageDateTime = getLastMessageDateTimeForNumber(mobileNumber);
				if (lastMessageDateTime != null && lastMessageDateTime.isBefore(LocalDateTime.now().minusHours(24)))
					;
				{
					updateFlagForNumbersInDatabase(mobileNumber, 0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateVisit(String mobileNumber) {
		try {
			updateLastMessageDateTimeForNumber(mobileNumber, LocalDateTime.now());
			updateFlagForNumbersInDatabase(mobileNumber, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<String> getMobileNumbersWithFlagOne() {
		List<String> mobileNumbers = new ArrayList<>();
		Query query = new Query(Criteria.where("flag").is(1));
		List<Document> documents = mongoTemplate.find(query, Document.class, "Sceduled_data");
		for (Document doc : documents) {
			mobileNumbers.add(doc.getString("Mobile Number"));
		}
		return mobileNumbers;
	}

	public LocalDateTime getLastMessageDateTimeForNumber(String mobileNumber) {
		Query query = new Query(Criteria.where("Mobile Number").is(mobileNumber));
		Document doc = mongoTemplate.findOne(query, Document.class, "Sceduled_data");
		if (doc != null && doc.containsKey("Last Message Date and Time")) {
			String dateString = doc.getString("Last Message Date and Time");
			LocalDateTime dateTime = LocalDateTime.parse(dateString,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return dateTime;
		}
		return null;
	}

	private void updateLastMessageDateTimeForNumber(String mobileNumber, LocalDateTime dateTime) {
		Query query = new Query(Criteria.where("Mobile Number").is(mobileNumber));
		Update update = Update.update("Last Message Date and Time",
				dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		mongoTemplate.updateFirst(query, update, "Sceduled_data");
	}

	private void updateFlagForNumbersInDatabase(String mobileNumber, int flag) {
		Query query = new Query(Criteria.where("Mobile Number").is(mobileNumber));
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime lastMessageDateTime = getLastMessageDateTimeForNumber(mobileNumber);

		if (lastMessageDateTime != null && lastMessageDateTime.isBefore(now.minusMinutes(5))) {
			Update update = Update.update("flag", flag).set("Last Message Date and Time",
					now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			mongoTemplate.updateFirst(query, update, "Sceduled_data");
		}
	}

	//@Scheduled(cron = "0 */1 * * * *")
	public void getFeedBackMessageDetails() {
		final String urlString = "https://webapi.chatio.io/api/inbox/get-inbox-details-page-wise?AccountId=181573&BusinessNumber=917981235279&AccPwd=&lastUpdateTime=";
		final String pdfFilePath = "C:/Users/aasif/Downloads/study/HyperApps/feedback_data.pdf";
		final String remoteFilePath = "https://noubabarandkitchen.in/noubabarandkitchen.in/najeeb/";
		try {
			RestTemplate restTemplate = new RestTemplate();
			Response responseData = restTemplate.getForObject(urlString, Response.class);
			if (responseData != null) {
				// processFeedbackData(responseData.getData());
				pdfGenerator.generateFeedbackPDF(responseData.getData(), pdfFilePath);
				// next upload in server.....
				ftpUploader.uploadFile(pdfFilePath, remoteFilePath);
				// sendPdfViaFacebookGraphApi(pdfFilePath, pdfFilePath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	 //@Scheduled(cron = "0 */1 * * * *")
	public void sendPdfViaFacebookGraphApi() {
		// here is the dummy attachment link which is present, in google drive..
		String mediaLink = "https://noubabarandkitchen.in/noubabarandkitchen.in/najeeb/feedback_data.pdf";

		try {
			String jsonPayload = "{" + "\"messaging_product\": \"whatsapp\"," + "\"to\": \"" + RECIPIENT_NUMBER + "\","
					+ "\"type\": \"document\"," + "\"document\": {" + "\"link\": \"" + mediaLink + "\","
					+ "\"filename\": \"my_pdf.pdf\"" + "}" + "}";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(ACCESS_TOKEN);

			HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(FACEBOOK_GRAPH_API_URL, request, String.class);

			System.out.println("Response status: " + response.getStatusCode());
			System.out.println("Response body: " + response.getBody());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
