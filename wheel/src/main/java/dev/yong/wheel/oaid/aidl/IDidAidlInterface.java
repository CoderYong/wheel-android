package dev.yong.wheel.oaid.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * 本文件代码根据以下AIDL生成，只改包名以便解决和移动安全联盟的SDK冲突问题：
 * <pre>
 *     // IDidAidlInterface.aidl
 *     package com.asus.msa.SupplementaryDID;
 *
 *     interface IDidAidlInterface {
 *
 *         boolean isSupport();
 *
 *         String getUDID();
 *
 *         String getOAID();
 *
 *         String getVAID();
 *
 *         String getAAID();
 *
 *     }
 * </pre>
 */
public interface IDidAidlInterface extends IInterface {

    /**
     * Local-side IPC implementation stub class.
     */
    abstract class Stub extends Binder implements IDidAidlInterface {

        private static final String DESCRIPTOR = "com.asus.msa.SupplementaryDID.IDidAidlInterface";

        static final int TRANSACTION_isSupport = IBinder.FIRST_CALL_TRANSACTION;
        static final int TRANSACTION_getOAID = IBinder.FIRST_CALL_TRANSACTION + 2;

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an repeackage.com.asus.msa.SupplementaryDID.IDidAidlInterface interface,
         * generating a proxy if needed.
         */
        public static IDidAidlInterface asInterface(IBinder service) {
            if (service == null) {
                return null;
            }
            IInterface iInterface = service.queryLocalInterface(DESCRIPTOR);
            if (iInterface instanceof IDidAidlInterface) {
                return (IDidAidlInterface) iInterface;
            }
            return new Proxy(service);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_isSupport: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean isSupport = isSupport();
                    reply.writeNoException();
                    reply.writeInt(isSupport ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getOAID: {
                    data.enforceInterface(DESCRIPTOR);
                    String oaid = getOAID();
                    reply.writeNoException();
                    reply.writeString(oaid);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements IDidAidlInterface {

            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public boolean isSupport() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                boolean result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isSupport, data, reply, 0);
                    reply.readException();
                    result = 0 != reply.readInt();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }

            @Override
            public String getOAID() throws RemoteException {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                String result;
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getOAID, data, reply, 0);
                    reply.readException();
                    result = reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
                return result;
            }
        }
    }

    boolean isSupport() throws RemoteException;

    String getOAID() throws RemoteException;
}
