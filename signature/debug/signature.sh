#!/bin/bash

# ת��ƽ̨ǩ������
# signs_debug.jks : ǩ���ļ�
# android : ǩ���ļ�����
# platform.pk8��platform.x509.pem : ϵͳǩ���ļ�
# platform : ǩ���ļ�����
./keytool-importkeypair -k signs_debug.jks -p 123456 -pk8 platform.pk8 -cert platform.x509.pem -alias platform