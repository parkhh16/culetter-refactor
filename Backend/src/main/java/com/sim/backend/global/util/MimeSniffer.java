package com.sim.backend.global.util;

public class MimeSniffer {
  public static String guessOrDefault(byte[] data, String def) {
    if (data.length >= 8) {
      // PNG
      if (data[0]==(byte)0x89 && data[1]==0x50 && data[2]==0x4E && data[3]==0x47) return "image/png";
      // JPEG
      if (data[0]==(byte)0xFF && data[1]==(byte)0xD8) return "image/jpeg";
      // MP3 (ID3 or MPEG frame sync)
      if ((data[0]=='I'&&data[1]=='D'&&data[2]=='3') || ((data[0]&0xFF)==0xFF && (data[1]&0xE0)==0xE0)) return "audio/mpeg";
      // GIF
      if (data[0]=='G'&&data[1]=='I'&&data[2]=='F') return "image/gif";
      // WEBP
      if (data[0]=='R'&&data[1]=='I'&&data[2]=='F'&&data[3]=='F' && data[8]=='W'&&data[9]=='E'&&data[10]=='B'&&data[11]=='P') return "image/webp";
      // TXT는 스니핑이 애매하니 기본값 사용
    }
    return def != null ? def : "application/octet-stream";
  }
}