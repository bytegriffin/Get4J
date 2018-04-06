package com.bytegriffin.get4j.probe;

public class ProbePage {

	private String url;
	private String content;
	private String status;

	public static ProbePage create(String url, String content, String status){
		return new ProbePage(url,content,status);
	}

	private ProbePage(String url,String content, String status){
		this.url = url;
		this.content =  content;
		this.status = status;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	

}
