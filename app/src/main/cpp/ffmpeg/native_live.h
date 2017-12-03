//
// Created by 李晓 on 17/8/23.
//

#ifndef FFMPEGTEST_NATIVE_LIVE_H
#define FFMPEGTEST_NATIVE_LIVE_H

#include "inc/rtmpdump/include/rtmp.h"

void add_rtmp_packet(RTMPPacket *packet);
void add_264_sequence_header(unsigned char *pps, unsigned char *sps, int pps_len, int sps_len);
void add_264_body(unsigned char *buf, int len);
void add_aac_sequence_header();
void add_aac_body(unsigned char *buf, int len);


#endif //FFMPEGTEST_NATIVE_LIVE_H
