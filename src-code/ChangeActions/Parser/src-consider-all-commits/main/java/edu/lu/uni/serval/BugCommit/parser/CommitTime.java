package edu.lu.uni.serval.BugCommit.parser;

public class CommitTime {
	private int year;
	private int month;
	private int day;
	private int hour;
	private int min;
	private int sec;
	
	// timeLine: 2009-06-24 15:44:47 +0000
	CommitTime(String timeLine){
		String[] times = timeLine.split(" ");
		this.year = Integer.parseInt(times[0].split("-")[0]);
		this.month = Integer.parseInt(times[0].split("-")[1]);
		this.day = Integer.parseInt(times[0].split("-")[2]);
		
		this.hour = Integer.parseInt(times[1].split(":")[0]);
		this.min = Integer.parseInt(times[1].split(":")[1]);
		this.sec = Integer.parseInt(times[1].split(":")[2]);
	}
	
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public int getMonth() {
		return month;
	}
	public void setMonth(int month) {
		this.month = month;
	}
	public int getDay() {
		return day;
	}
	public void setDay(int day) {
		this.day = day;
	}
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	public int getMin() {
		return min;
	}
	public void setMin(int min) {
		this.min = min;
	}
	public int getSec() {
		return sec;
	}
	public void setSec(int sec) {
		this.sec = sec;
	}
	
	
}
