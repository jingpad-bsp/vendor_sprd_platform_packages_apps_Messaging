1������libs/ctcc/ims.jar���е�org.mozilla��� libs/cucc/rhino-1.7.7.1.jar�е�org.mozilla���ظ��ˣ�������libs/cucc/rhino-1.7.7.1.jar��ֻ��org.mozilla�࣬���԰�libs/cucc/rhino-1.7.7.1.jar������libs/cucc/rhino-1.7.7.1.jar.bak����ʹ��������
   libs/cmcc/smsparsing/android-support-v4.jar �ڹ������Ѿ����ˣ�����Ҳȥ��������ᵼ�±��뱨�� 
				   ����: �Ҳ�������
						����:   ���� setChannelId(String)
						λ��: ����ΪBuilder�ı��� builder	
						
2��Ϊ�˽��res��res_sprd�е���Դ��ͻ���ٿ��ǵ�res_sprd��res���ȼ��ߣ����԰�res_sprd�ֱ�ŵ�debug��release��ȥ

3��res_cmcc_sso��res_smsparsing���кܶ���Դ�ظ���������⣬��ͻ����Դ��ֵ����ͬ������ע�͵�res_smsparsing\valuesĿ¼�е��������ļ�smssdk_colors.xml��smssdk_dimens.xml��smssdk_ids.xml�ж������Դ��

4�����һ��JNI��������һ����̬�⣨��̬�⣩��������Android.mk�����ӣ�
		LOCAL_STATIC_LIBRARIES ��= libxxx
		
		��Ҫ�����Android.mk���������������һ�д��룺
		include $(call all-makefiles-under, $(libs_path))   //libs_path �����������·��
		
		��libframesequenceΪ����libframesequence������libgif��̬�⣬������Ҫ��Android.mk�ļ������ӣ�
		LOCAL_STATIC_LIBRARIES := libgif
		ͬʱ����Android.mk�ļ���������ӣ�
		include $(call all-makefiles-under, $(LIBS_PATH))   //LIBS_PATH �����������·��,����Ŀ�·����libs
		
5����̬���ʹ��˵��
		��ּ����Ҫ��������������ֱ�����Android Studio����ͻ��ڹ��̱��루ʹ��make messaging�����Ҫ�����ܼ�������������������ܵĸ��þ�̬�⡣
		�漰���ľ�̬����
				chips(com.android.ex.chips): frameworks/opt/chips
				common(com.android.common): frameworks/ex/common
				framesequence(android.support.rastermill): frameworks/ex/framesequence
				photoviewer(com.android.ex.photo): frameworks/opt/photoviewer
				vcard(com.android.vcard): frameworks/opt/vcard
				giflib: external/giflib  
				guava: external/guava
				jsr305: external/jsr305
				libphonenumber: external/libphonenumber
				recyclerview-v7
				appcompat-v7
				�� giflib �� framesequence: ��Ϊ�漰��Android.mk�ļ����޸ģ�Ϊ�˲��޸������ֿ��Android.mk�ļ�, AS����ͻ��ڹ��̱����ʹ��Messaging/libs/����Ŀ�
				����chips���в��ȶ���AS�ͻ��ڹ��̱���ʱͳһ����Messaging/libs/chips���Է�������ά����
				����֮�⣬�������������еĿ⡣
				ע�⣺ ���׼����Messging����������Ŀ¼�£���ŵ����ش���D:\)����,����Ҫͨ�� File -> New -> Import Module�ķ�ʽ������Ӧ�ľ�̬��(common��photoviewer��vcard��guava��jsr305��libphonenumber)��������������ʱ�򣬻ὫԴ�뿽����appͬ��Ŀ¼���ٽ������Ƶ�libsĿ¼���ٽ�settings.gradle��useLocalLibs��ֵ����Ϊtrue
				��ʾ���ڹ���Ŀ¼��vendor\sprd\platform\packages\apps\Messaging��������ԣ���Ҫ���뾲̬�⣬������ִ��make messagingʱ����ֿ��ظ�����Ĵ���
				
		�漰�Ķ�̬����
				libs/framework.jar
    		libs/telephony-common.jar
    		libs/radio_interactor_common.jar
    ��Ҫ��Ϊ���ܹ��������������ģ���������sprdroid9.0_trunk

6��framesequence ��ϵͳ���ظ������ѽ�framesequence�����ļ���ɾ������������Ϊframesequence.7zѹ���ļ�������libs�ļ���·���¡���Messaging�ļ�����
     ��Android.mk��MmsFolderView/Android.mk�ļ��е�libframesequence-ex��android-common-framesequence-ex�ֱ��޸�Ϊlibframesequence��android-common-framesequence���޸ĺ��sprdroidq_trunk��
     ����ͨ��AS���룬����AS������ԣ����轫framesequence.7z��ѹ���ͽ�ѹ�ڵ�ǰ·���£�������Messaging�ļ����µ�Android.mk��MmsFolderView/Android.mk���ļ��ָ���֮ǰ�������½�libframesequence�޸�
     Ϊlibframesequence-ex��android-common-framesequence�޸�Ϊandroid-common-framesequence-ex��

