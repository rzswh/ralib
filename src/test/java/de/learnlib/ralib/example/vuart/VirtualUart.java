package de.learnlib.ralib.example.vuart;

import org.testng.Assert;
import org.testng.annotations.Test;

public class VirtualUart {

    private byte recvBuf = 0;
    private byte sendBuf = 0;
    private byte recvReg = 0;
    private byte sendReg = 0;

    private boolean exSending = false;
    private boolean exToRecv = false;
    private boolean internalSending = false;
    private boolean internalRecved = false;
    
    public void externalSend(Byte b) {
        recvBuf = b;
        exSending = true;
    }

    public Byte externalRecv() {
        if (!exToRecv) {
            throw new IllegalAccessError("empty");
        }
        exToRecv = false;
        return sendBuf;
    }

    public void mmioSend(Byte b) {
        sendReg = b;
        internalSending = true;
    }

    public Byte mmioRecv() {
        byte ret = recvReg;
        recvReg = 0;
        internalRecved = false;
        return ret;
    }

    public void waitT() {
        if (internalSending) {
            sendBuf = sendReg;
            internalSending = false;
            exToRecv = true;
        }
        if (exSending) {
            recvReg = recvBuf;
            exSending = false;
            internalRecved = true;
        }
    }

    public Byte getFlags() {
        byte flag = 0;
        if (internalSending) flag |= 1;
        if (internalRecved) flag |= 2;
        return flag;
    }

    @Test
    public static void testVirtualUart() {
        VirtualUart vuart = new VirtualUart();
        vuart.externalSend((byte)12);
        vuart.waitT();
        try {
            vuart.externalRecv();
            Assert.fail("null");
        } catch (IllegalAccessError e) {}
        Assert.assertEquals(vuart.getFlags().intValue(), 0x2);
        Assert.assertEquals(vuart.mmioRecv().intValue(), 12);
        Assert.assertEquals(vuart.getFlags().intValue(), 0x0);
        vuart.mmioSend((byte)24);
        Assert.assertEquals(vuart.getFlags().intValue(), 0x1);
        vuart.waitT();
        try {
            vuart.mmioRecv();
            Assert.fail("null");
        } catch (IllegalAccessError e) {}
        Assert.assertEquals(vuart.externalRecv().intValue(), 24);
        Assert.assertEquals(vuart.getFlags().intValue(), 0x0);

        vuart = new VirtualUart();
        vuart.mmioSend((byte)0);
        vuart.mmioSend((byte)0);
        vuart.waitT();
        try {
            vuart.mmioRecv();
            Assert.fail();
        } catch (IllegalAccessError e) {
        }
    }
}
