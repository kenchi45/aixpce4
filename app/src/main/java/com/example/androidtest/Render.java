package com.example.androidtest;

import android.graphics.Bitmap;

public class Render {
	private Bitmap bitmap;
	private int screenWidth;
	private int screenHeight;
	
	private int [] buffer;
	private int offXPce;
	private int offYPce;
	private int widthPce;
	private int heightPce;

	public Render(int width, int height) {
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		screenWidth = width;
		screenHeight = height;
	}

	public void setDrawRequest(int[] buffer, int offXPce, int offYPce, int widthPce, int heightPce) {
		this.buffer = buffer;
		this.offXPce = offXPce;
		this.offYPce = offYPce;
		this.widthPce = widthPce;
		this.heightPce = heightPce;
	}
	
	public void draw(int screenWidth, int screenHeight) {
		// TODO: とりあえず
//		switch (Config.ScreenSetting) {
//			case Config.SCREEN_SETTING_NORMAL:
//				if (Config.ScreenRotate == 0) {
//					drawVramImage();
//				} else {
//					drawVramImageRotate();
//				}
//				break;
//
//			case Config.SCREEN_SETTING_SCALED:
//				drawVramImageScaled();
//				break;
//
//			case Config.SCREEN_SETTING_SCALED2:
//				drawVramImageScaled2();
//				break;
//
//			default:
//				drawVramImage();
//				break;
//		}
		drawVramImage();
	}

	public Bitmap getBitmap() {
		return bitmap;
	}
	
	private void drawVramImage() {
		if (widthPce <= 0 || heightPce <= 0) {
			return;
		}
		int i;
		int start = offYPce * Pce.WIDTH + offXPce;
		int end = heightPce * Pce.WIDTH;
		int renderOffX = (screenWidth - widthPce) / 2;
		int renderOffY = (screenHeight - heightPce) / 2;
		for (i = start; i < end; i += Pce.WIDTH, renderOffY++) {
			//g.setPixels(renderOffX, renderOffY, widthPce, 1, buffer, i);
			bitmap.setPixels(buffer, i, widthPce, renderOffX, renderOffY, widthPce, 1);
		}
		//bitmap.setPixels(buffer, 0, widthPce, 0, 0, widthPce, heightPce);
	}

//	public void drawVramImageRotate() {
//		if (widthPce <= 0 || heightPce <= 0) {
//			return;
//		}
//
//		if (image == null
//				|| image.getWidth() != widthPce
//				|| image.getHeight() != heightPce) {
//			image = Image.createImage(widthPce, heightPce);
//			gImg = image.getGraphics();
//			gImg.setColor(Graphics.getColorOfName(Graphics.BLACK));
//			gImg.fillRect(0, 0, image.getWidth(), image.getHeight());
//		}
//
//		int i;
//		int start = offYPce * Pce.WIDTH + offXPce;
//		int end = heightPce * Pce.WIDTH;
//		int y = 0;
//		for (i = start; i < end; i += Pce.WIDTH, y++) {
//			gImg.setPixels(0, y, widthPce, 1, buffer, i);
//		}
//
//		switch (Config.ScreenRotate) {
//			case 0:
//				g.setFlipMode(Graphics.FLIP_NONE);
//				break;
//			case 1:
//				g.setFlipMode(Graphics.FLIP_ROTATE_RIGHT);
//				break;
//			case 2:
//				g.setFlipMode(Graphics.FLIP_ROTATE);
//				break;
//			case 3:
//				g.setFlipMode(Graphics.FLIP_ROTATE_LEFT);
//				break;
//		}
//		g.drawImage(image, offXPce, offYPce);
//		g.setFlipMode(Graphics.FLIP_NONE);
//	}
	
//	public void drawVramImageScaled() {
//		if (widthPce <= 0 || heightPce <= 0) {
//			return;
//		}
//
//		if (image == null
//				|| image.getWidth() != widthPce
//				|| image.getHeight() != heightPce) {
//			image = Image.createImage(widthPce, heightPce);
//			gImg = image.getGraphics();
//			gImg.setColor(Graphics.getColorOfName(Graphics.BLACK));
//			gImg.fillRect(0, 0, image.getWidth(), image.getHeight());
//		}
//
//		int i;
//		int start = offYPce * Pce.WIDTH + offXPce;
//		int end = heightPce * Pce.WIDTH;
//		int y = 0;
//		for (i = start; i < end; i += Pce.WIDTH, y++) {
//			gImg.setPixels(0, y, widthPce, 1, buffer, i);
//		}
//
//		switch (Config.ScreenRotate) {
//			case 0:
//				g.setFlipMode(Graphics.FLIP_NONE);
//				break;
//			case 1:
//				g.setFlipMode(Graphics.FLIP_ROTATE_RIGHT);
//				break;
//			case 2:
//				g.setFlipMode(Graphics.FLIP_ROTATE);
//				break;
//			case 3:
//				g.setFlipMode(Graphics.FLIP_ROTATE_LEFT);
//				break;
//		}
//		g.drawScaledImage(image, 0, 0, screenWidth, screenHeight, 0, 0, widthPce, heightPce);
//		g.setFlipMode(Graphics.FLIP_NONE);
//	}
//
//	public void drawVramImageScaled2() {
//		if (widthPce <= 0 || heightPce <= 0) {
//			return;
//		}
//
//		if (image == null
//				|| image.getWidth() != widthPce
//				|| image.getHeight() != heightPce) {
//			image = Image.createImage(widthPce, heightPce);
//			gImg = image.getGraphics();
//			gImg.setColor(Graphics.getColorOfName(Graphics.BLACK));
//			gImg.fillRect(0, 0, image.getWidth(), image.getHeight());
//		}
//
//		int i;
//		int start = offYPce * Pce.WIDTH + offXPce;
//		int end = heightPce * Pce.WIDTH;
//		int y = 0;
//		for (i = start; i < end; i += Pce.WIDTH, y++) {
//			gImg.setPixels(0, y, widthPce, 1, buffer, i);
//		}
//
//		switch (Config.ScreenRotate) {
//			case 0:
//				g.setFlipMode(Graphics.FLIP_NONE);
//				break;
//			case 1:
//				g.setFlipMode(Graphics.FLIP_ROTATE_RIGHT);
//				break;
//			case 2:
//				g.setFlipMode(Graphics.FLIP_ROTATE);
//				break;
//			case 3:
//				g.setFlipMode(Graphics.FLIP_ROTATE_LEFT);
//				break;
//		}
//
//		int renderWidth = (int)(widthPce * ((double)screenHeight / heightPce));
//		int renderHeight = screenHeight;
//
//		int dx = (screenWidth - renderWidth) / 2;
//		int dy = (screenHeight - renderHeight) / 2;
//		g.drawScaledImage(image, dx, dy, renderWidth, renderHeight, 0, 0, widthPce, heightPce);
//		g.setFlipMode(Graphics.FLIP_NONE);
//	}
}
