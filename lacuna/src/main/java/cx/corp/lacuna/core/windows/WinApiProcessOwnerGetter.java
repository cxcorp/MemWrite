package cx.corp.lacuna.core.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import cx.corp.lacuna.core.windows.winapi.Advapi32;
import cx.corp.lacuna.core.windows.winapi.SystemErrorCode;
import cx.corp.lacuna.core.windows.winapi.WinApiConstants;

import java.util.Optional;

public class WinApiProcessOwnerGetter implements ProcessOwnerGetter {

    private final Advapi32 advapi;

    public WinApiProcessOwnerGetter(Advapi32 advapi) {
        if (advapi == null) {
            throw new IllegalArgumentException("advapi cannot be null!");
        }
        this.advapi = advapi;
    }

    @Override
    public Optional<String> get(ProcessHandle processHandle) {
        if (processHandle == null) {
            throw new IllegalArgumentException("processHandle cannot be null!");
        }

        return getProcessToken(processHandle.getNativeHandle())
            .flatMap(this::getTokenUser)
            .flatMap(this::getUserName);
    }

    private Optional<Integer> getProcessToken(int processHandle) {
        IntByReference token = new IntByReference(0);
        boolean success =
            advapi.openProcessToken(
                processHandle,
                WinApiConstants.OPENPROCESSTOKEN_TOKEN_QUERY,
                token);
        return success ? Optional.of(token.getValue()) : Optional.empty();
    }

    private Optional<Advapi32.TokenUser> getTokenUser(int processToken) {
        // First find out how big of a buffer we need, then get the information
        // with a properly sized buffer.
        return getTokenInfoBufferLength(processToken)
            .flatMap(bufLen -> lookupTokenUser(processToken, bufLen));
    }

    private Optional<String> getUserName(Advapi32.TokenUser user) {
        // includes null terminator!
        return getUsernameBufferLength(user)
            .flatMap(bufLen -> lookupTokenUserName(user, bufLen));
    }

    private Optional<Integer> getTokenInfoBufferLength(int processToken) {
        IntByReference bytesNeeded = new IntByReference(0);
        boolean success =
            advapi.getTokenInformation(
                processToken,
                WinApiConstants.GETTOKENINFORMATION_TOKENUSER,
                null,
                0,
                bytesNeeded);

        return !success && callFailedBecauseBufferWasTooSmall()
            ? Optional.of(bytesNeeded.getValue())
            : Optional.empty();
    }

    private Optional<Integer> getUsernameBufferLength(Advapi32.TokenUser user) {
        IntByReference nameLength = new IntByReference(0);
        IntByReference domainLength = new IntByReference(0);
        IntByReference ignored = new IntByReference(0);

        boolean success =
            advapi.lookupAccountSidW(
                WinApiConstants.NULLPTR,
                user.user,
                null,
                nameLength,
                null,
                domainLength,
                ignored);

        return !success && callFailedBecauseBufferWasTooSmall()
            ? Optional.of(nameLength.getValue())
            : Optional.empty();
    }

    private boolean callFailedBecauseBufferWasTooSmall() {
        return Native.getLastError() == SystemErrorCode.INSUFFICIENT_BUFFER.getSystemErrorId();
    }

    private Optional<Advapi32.TokenUser> lookupTokenUser(int token, int infoBufferLength) {
        Memory memory = new Memory(infoBufferLength);
        IntByReference bufferLen = new IntByReference(infoBufferLength);
        boolean success =
            advapi.getTokenInformation(
                token,
                WinApiConstants.GETTOKENINFORMATION_TOKENUSER,
                memory,
                (int) memory.size(),
                bufferLen);

        if (!success) {
            return Optional.empty();
        }

        try {
            Advapi32.TokenUser user = new Advapi32.TokenUser(memory);
            return Optional.of(user);
        } catch (Error | Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<String> lookupTokenUserName(Advapi32.TokenUser user, int nameBufferLenWithNullTerminator) {
        char[] nameBuffer = new char[nameBufferLenWithNullTerminator];
        IntByReference nameBufNeededLen = new IntByReference(nameBuffer.length);
        char[] domainBuffer = new char[WinApiConstants.MAX_DOMAIN_NAME_LENGTH];
        IntByReference domainBufNeededLen = new IntByReference(domainBuffer.length);
        IntByReference ignored = new IntByReference(0);

        boolean success =
            advapi.lookupAccountSidW(
                WinApiConstants.NULLPTR,
                user.user,
                nameBuffer,
                nameBufNeededLen,
                domainBuffer,
                domainBufNeededLen,
                ignored);

        int lengthWithoutNullTerminator = nameBufferLenWithNullTerminator - 1;
        return success
            ? Optional.of(new String(nameBuffer, 0, lengthWithoutNullTerminator))
            : Optional.empty();
    }
}
