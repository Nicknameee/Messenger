package spring.application.tree.data.chats.attributes;

import org.springframework.http.HttpStatus;
import spring.application.tree.data.exceptions.InvalidAttributesException;

import java.time.LocalDateTime;
import java.util.Arrays;

public enum ChatType {
    DIALOGUE, GROUP, CHANNEL, PERSONAL, BOT;

    public boolean isUserAddingAllowed(int membersCount) throws InvalidAttributesException {
        if (membersCount < 0) {
            throw new InvalidAttributesException(String.format("Members count value is invalid: %s", membersCount),
                                                 Arrays.asList(Thread.currentThread().getStackTrace()).get(1).toString(),
                                                 LocalDateTime.now(), HttpStatus.NOT_ACCEPTABLE);
        }
        if (Arrays.asList(GROUP, CHANNEL).contains(this)) {
            return true;
        }
        if (this == PERSONAL) {
            return membersCount == 0;
        }
        return membersCount < 2;
    }
}
