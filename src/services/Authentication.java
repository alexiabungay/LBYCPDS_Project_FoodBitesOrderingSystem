package services;

import features.*;

public class Authentication {
    private final DataStoring store;
    private User currentUser;

    public Authentication(DataStoring store) {
        this.store = store;
    }

}
