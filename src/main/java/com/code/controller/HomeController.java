package com.code.controller;

import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.code.service.DataService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RestController
@RequestMapping("api/v1")
public class HomeController {
	@Autowired
	DataService dataService;

	@Autowired
	private MongoTemplate mongoTemplate;

	@GetMapping
	public String healthCheck() {
		return "Application working fine";
	}
	
	  @GetMapping("/logs")
	    public List<Document> getSchedulerLogs() {
	        return mongoTemplate.findAll(Document.class, "Sceduled_data");
	    }

	@Scheduled(cron = "0 */1 * * * *")
	public void getInboxDetails() {
		String url = "https://webapi.chatio.io/api/inbox/get-inbox-details-page-wise?AccountId=181573&BusinessNumber=917981235279&AccPwd=&lastUpdateTime=";
		try {
			List<Document> documents = dataService.getDataFromURL(url);
			if (!documents.isEmpty()) {
				saveDocumentsToDatabase(documents);
				log.info("Retrieved {} documents from the API.", documents.size());

			}
		} catch (Exception e) {
			handleException(e);
		} finally {
			dataService.sendMessage();
			try {
				// for 5 mins sleep we will change in real time...
				Thread.sleep(300000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		}
	}

	private void saveDocumentsToDatabase(List<Document> documents) {
		mongoTemplate.insert(documents, "Sceduled_data");
	}

	private void handleException(Exception e) {
		log.debug("An exception occurred during scheduled task execution: {}", e.getMessage());
	}

	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

}
