package com.example.androidtest;

public class RomTitle {
	private String romTitle;
	private String hash;
	
	public RomTitle(String romTitle, String hash) {
		this.romTitle = romTitle;
		this.hash = hash;
	}
	
	public String getRomTitle() {
		return this.romTitle;
	}
	
	public void setRomTitle(String romTitle) {
		this.romTitle = romTitle;
	}
	
	public String getHash() {
		return this.hash;
	}
	
	public void setHash(String hash) {
		this.hash = hash;
	}
}
