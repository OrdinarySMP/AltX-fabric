package com.xadale.playerlogger.repositories.implementations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.xadale.playerlogger.data.AuthorizedAccounts;
import com.xadale.playerlogger.repositories.AuthorizedAccountsRepository;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class JsonAuthorizedAccountsRepositoryImpl implements AuthorizedAccountsRepository {

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final Logger LOGGER = LogUtils.getLogger();
  private final File file;
  private List<AuthorizedAccounts> data;

  private JsonAuthorizedAccountsRepositoryImpl(File file) {
    this.file = file;
    if (!file.exists()) {
      this.data = new ArrayList<>();
      return;
    }
    try (FileReader reader = new FileReader(file)) {
      this.data = gson.fromJson(reader, new TypeToken<ArrayList<AuthorizedAccounts>>() {});
    } catch (JsonSyntaxException ex) {
      throw new RuntimeException("Cannot parse data in %s".formatted(file.getAbsolutePath()), ex);
    } catch (Exception e) {
      throw new RuntimeException("Cannot load data (%s)".formatted(file.getAbsolutePath()), e);
    }
    if (this.data == null) {
      // gson failed to parse a valid list
      this.data = new ArrayList<>();
    }
    this.data.forEach(data -> data.setRepository(this));
    System.out.println("Loaded " + this.data.size() + " data entries");
  }

  public static AuthorizedAccountsRepository from(File file) {
    return new JsonAuthorizedAccountsRepositoryImpl(file);
  }

  @Override
  public Optional<AuthorizedAccounts> get(UUID uuid) {
    return this.data.stream().filter(authAlts -> authAlts.getUuids().contains(uuid)).findFirst();
  }

  @Override
  public void add(AuthorizedAccounts authorizedAccounts) {

    List<AuthorizedAccounts> authorizedAccountsList =
        authorizedAccounts.getUuids().stream()
            .map(this::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    // none authorized already
    if (authorizedAccountsList.isEmpty()) {
      this.data.add(authorizedAccounts);
      authorizedAccounts.setRepository(this);
      this.onUpdated();
      return;
    }

    // all authorized with each other already
    if (new HashSet<>(authorizedAccountsList.getFirst().getUuids())
        .containsAll(authorizedAccounts.getUuids())) {
      return;
    }

    Set<UUID> uuidSet = new HashSet<>(authorizedAccounts.getUuids());
    for (AuthorizedAccounts authAlts : authorizedAccountsList) {
      uuidSet.addAll(authAlts.getUuids());
      this.remove(authAlts);
    }
    AuthorizedAccounts newAuthorizedAccounts = new AuthorizedAccounts(new ArrayList<>(uuidSet));
    this.add(newAuthorizedAccounts);
  }

  @Override
  public void remove(AuthorizedAccounts authorizedAccounts) {
    this.data.remove(authorizedAccounts);
    this.onUpdated();
  }

  @Override
  public void update(AuthorizedAccounts authorizedAccounts) {
    this.onUpdated();
  }

  private void onUpdated() {
    try (FileWriter writer = new FileWriter(this.file)) {
      gson.toJson(this.data, writer);
    } catch (IOException e) {
      LOGGER.error("Failed to save data to file {}", this.file.getAbsolutePath(), e);
    }
  }

  @Override
  public Stream<AuthorizedAccounts> getAll() {
    return this.data.stream();
  }
}
