package com.xadale.playerlogger.core;

public abstract class AbstractData<T extends AbstractData<T, ID>, ID> {
  private transient DataRepository<T, ID> dataRepository;

  /**
   * Returns the current Repository instance associated with this IpAss object.
   *
   * @return The Repository instance used for data operations
   */
  public DataRepository<T, ID> getRepository() {
    return this.dataRepository;
  }

  /**
   * Sets the Repository instance to be associated with this IpAss object.
   *
   * @param dataRepository The Repository instance to be used for data operations
   */
  public void setRepository(DataRepository<T, ID> dataRepository) {
    this.dataRepository = dataRepository;
  }
}
