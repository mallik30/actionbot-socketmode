package com.arjun.slack.api;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class ActionItem {
	
	String requestId;
	String description;
	String priority;
	String assignedUserId;
	String eta;
	String createdBy;
	LocalDateTime createdTimestamp;
	LocalDateTime lastUpdatedTimestamp;
	Status status;
	String channelId;
	String responseURL;
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getAssignedUserId() {
		return assignedUserId;
	}
	public void setAssignedUserId(String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}
	public String getEta() {
		return eta;
	}
	public void setEta(String eta) {
		this.eta = eta;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public LocalDateTime getCreatedTimestamp() {
		return createdTimestamp;
	}
	public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}
	public LocalDateTime getLastUpdatedTimestamp() {
		return lastUpdatedTimestamp;
	}
	public void setLastUpdatedTimestamp(LocalDateTime lastUpdatedTimestamp) {
		this.lastUpdatedTimestamp = lastUpdatedTimestamp;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	public String getResponseURL() {
		return responseURL;
	}
	public void setResponseURL(String responseURL) {
		this.responseURL = responseURL;
	}
	@Override
	public String toString() {
		return "ActionItem [requestId=" + requestId + ", description=" + description + ", priority=" + priority
				+ ", assignedUserId=" + assignedUserId + ", eta=" + eta + ", createdBy=" + createdBy
				+ ", createdTimestamp=" + createdTimestamp + ", lastUpdatedTimestamp=" + lastUpdatedTimestamp
				+ ", status=" + status + ", channelId=" + channelId + ", responseURL=" + responseURL + "]";
	}
	

}
