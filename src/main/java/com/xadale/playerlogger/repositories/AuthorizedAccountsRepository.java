package com.xadale.playerlogger.repositories;

import com.xadale.playerlogger.core.DataRepository;
import com.xadale.playerlogger.data.AuthorizedAccounts;
import java.util.UUID;

public interface AuthorizedAccountsRepository extends DataRepository<AuthorizedAccounts, UUID> {}
