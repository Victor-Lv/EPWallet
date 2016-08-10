/**MyPin.java
 * @����         ����
 * @������ 2016-8-8 ����10:37:51
 * @�汾    1.0
 */
package my;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class MyPin
{
	private byte[] pinValue;
	private byte tryLimit;
	private byte pinSize;
	private byte tryCounter; //��ʣ���Ի���,Ĭ��Ϊ3
	private boolean[] checkPin; //falseʱ��ʾ��δ��֤pin
	private boolean[] blockPin;//��ֱ����tryCounter==0����,����Ϊ�����˶���Ӹ�����
	
	MyPin()
	{
		this.pinValue = new byte[8]; //8�ֽڵ�PINValue,�Զ���ʼ��Ϊ00...
		//ȷ����ʼ��Ϊ00...
		Util.arrayFillNonAtomic(this.pinValue, (short)0, (short)8, (byte)0);//����ֽ����麯��
		
		this.tryLimit = (byte)3;
		this.pinSize = (byte)8;
		this.tryCounter = this.tryLimit;
		//�����º������ٿռ�ʱ���Զ���ʼ��Ϊfalse
		this.checkPin = JCSystem.makeTransientBooleanArray((short)1, JCSystem.CLEAR_ON_DESELECT);//deselect��reset�Զ���λ
		this.blockPin = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_DESELECT);//�ṩ������ʽ������reset�����Զ�����
		
		/**������
		 * 1.֮����Ҫ�ѱ�־λ��װ��1�ֽ����飬����Ϊ��Ҫ�ṩ�Զ���λ�ķ�����
		 *   Ҳ��������reset��APDUʱ��������Ҫ����Ӧ�ı�־λ�Զ���λ��
		 * 2.JCSystem.makeTransientBooleanArray()�������ṩ�������һ���Զ���λ�Ĺ���,
		 * 3.ͬʱע�����������������ʼ��Ϊfalse
		 * 4.reset�������deselect���������
		 */
	}
	
	public boolean pinVerify()
	{
		return this.checkPin[0];
	}

	public byte check(byte[] pin,
					  short offset,
					  byte length)
	 				  throws ArrayIndexOutOfBoundsException,
	 						 NullPointerException
	{
		if(length != this.pinSize) //���볤�Ȳ�����Ҫ���PIN����
		{
			this.tryCounter--;
			if(this.tryCounter == (byte)0)
				this.blockPin[0] = true;
			return this.tryCounter;
		}
		byte result = Util.arrayCompare(pin, offset, this.pinValue, (short)0, (short)8);
		if(result == (byte)0) //���
		{
			this.tryCounter = this.tryLimit;
			this.checkPin[0] = true;
			return this.tryLimit;
		}
		else
		{
			this.tryCounter--;
			if(this.tryCounter == (byte)0)
				this.blockPin[0] = true;
			return this.tryCounter;
		}
	}
	
	public boolean setPin(byte[] pin,
						  short offset,
						  byte length)
		 				  throws ArrayIndexOutOfBoundsException,
		 						 NullPointerException
	{
		/*block �� verify��Щ��֤������ǰ����*/
		if(length != this.pinSize)
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		Util.arrayCopyNonAtomic(pin, offset, this.pinValue, (short)0, length);
		return true;
	}
	
	public boolean pinBlock()
	{
		return blockPin[0];
	}
	
}
