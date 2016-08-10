/**Wallet.java
 * @����         ����
 * @������ 2016-8-8 ����10:37:51
 * @�汾    1.0
 */
package my;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

public class Wallet extends Applet {
	private short balance;//���,���Ϊ7FFF(ʮ�����Ʊ�ʾ)
	private MyPin myPin;
	//short������byte�ֽ�
	
	Wallet()
	{
		myPin = new MyPin();
		balance = (short)0;
	}
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Wallet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}
	//��֤pin
	public void checkPin(APDU apdu)
	{
		if(myPin.pinBlock())
			ISOException.throwIt((short)0x6283);
		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();
		byte tryCounter = myPin.check(buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
		if(tryCounter == (byte)2)
			ISOException.throwIt((short)0x63C2);
		else if(tryCounter == (byte)1)
			ISOException.throwIt((short)0x63C1);
		else if(tryCounter == (byte)0)
			ISOException.throwIt((short)0x63C0);
	}
	//�޸�pin
	public void setNewPin(APDU apdu)
	{
		if(myPin.pinBlock())
			ISOException.throwIt((short)0x6283);
		if(!myPin.pinVerify()) //δ��֤pin
			ISOException.throwIt((short)0x6982);
		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();
		myPin.setPin(buf, ISO7816.OFFSET_CDATA, buf[ISO7816.OFFSET_LC]);
	}
	//��ȡ���
	public void getBalance(APDU apdu)
	{
		if(myPin.pinBlock())
			ISOException.throwIt((short)0x6283);
		if(!myPin.pinVerify())
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		byte[] buf = apdu.getBuffer();
		
		//�ؼ�����,��short��ʾ��ʮ������ת��(���)��ʮ�����Ƶ���byte���ֽ�����
		Util.setShort(buf, (short)ISO7816.OFFSET_CDATA, balance);
		
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short)2);
	}
	
	//�������
	public void increase(byte[] buf, short bufOffset)
	{
		short temp = Util.getShort(buf, (short)ISO7816.OFFSET_CDATA);//��2�ֽڵ������ʾ��ʮ��������ת����shortʮ������
		if(temp < 0)//���˸����������쳣
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		temp += this.balance;
		if(temp < (short)0) //����洢
		{
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		else
		{
			this.balance = temp;
		}
	}
	
	//��ȥ���
	public void decrease(byte[] buf, short bufOffset)
	{
		short temp = Util.getShort(buf, (short)ISO7816.OFFSET_CDATA);//��2�ֽڵ������ʾ��ʮ��������ת����shortʮ������
		if(temp < 0)//���˸����������쳣
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		if(temp > this.balance) //��������
		{
			this.balance = (short)0; //����������ֵ
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED); //Ȼ�����쳣˵���޶�
		}
		else
		{
			this.balance -= temp;
		}
	}
	//��ֵ
	public void charge(APDU apdu)
	{
		if(myPin.pinBlock())
			ISOException.throwIt((short)0x6283);
		if(!myPin.pinVerify())
			ISOException.throwIt((short)0x6982);
		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();//���ǵñ�©��,©�˻ᵼ�����Է��ֵ�bug!ģ�����ܿ���û���⵫�ŵ���Ƭ�ܳ���ͻ������ֵĽ��!
		if(buf[ISO7816.OFFSET_LC] != (byte)2) //���������ֽ�����������
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		increase(buf,(short)ISO7816.OFFSET_CDATA);
	}
	//����
	public void consume(APDU apdu)
	{
		if(myPin.pinBlock())
			ISOException.throwIt((short)0x6283);
		if(!myPin.pinVerify())
			ISOException.throwIt((short)0x6982);
		byte[] buf = apdu.getBuffer();
		apdu.setIncomingAndReceive();
		if(buf[ISO7816.OFFSET_LC] != (byte)2)
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		decrease(buf,(short)ISO7816.OFFSET_CDATA);
	}
	
	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}
		
		//byte->short��ת��ʵ���ǽ�һ��0/1�Ķ�������ת����ʮ������
		/*byte[] temp = {11,22};
		short bal = Util.getShort(temp, (short)0);
		Util.setShort(temp, (short)0, (short)1032);*/
		
		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) 0x20:
			checkPin(apdu);
			break;
		case (byte) 0x21:
			setNewPin(apdu);
			break;
		case (byte) 0x10:
			getBalance(apdu);
			break;
		case (byte) 0x11:
			charge(apdu);
			break;
		case (byte) 0x12:
			consume(apdu);
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
}

/**
 * �ر�ע��㣺
 * apdu.setIncomingAndReceive();���©�����������ܻᵼ���޷���ȡ��������data�򣬻��߻�ȡ���Ķ���0.
 * ������process()�����ĳ��caseҪ�õ�data��ģ�����봫��һ��APDU�����ȥ����������
 * ���Ǵ������в��õ�data����switchֻ�õ�INS�򣩡�
 * ������ֻ����һ��������buf��ȥ,��Ϊ����������process��дapdu.setIncomingAndReceive();���
 * ������׵��������ᵽ���⣺
 * �ᵼ�����Է��ֵ�bug!ģ�����ܿ���û���⵫�ŵ���Ƭ�ܳ���ͻ������ֵĽ��!
 */
