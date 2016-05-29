DROP TABLE IF EXISTS ServerRooms;
DROP TABLE IF EXISTS Servers;
DROP TABLE IF EXISTS Rooms;

CREATE TABLE Rooms (
  id                      INTEGER NOT NULL PRIMARY KEY,
  name                    TEXT NOT NULL,
  owner                   TEXT NOT NULL,
  password                TEXT NULL,
  timestamp			      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Servers (
  id                     INTEGER NOT NULL PRIMARY KEY,
  address                TEXT NOT NULL,
  http                   INTEGER NOT NULL,
  tcp                    INTEGER NOT NULL,
  timestamp              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ServerRooms (
  server                 INTEGER NOT NULL,
  room                   INTEGER NOT NULL,
  FOREIGN KEY(server)    REFERENCES Servers(id),
  FOREIGN KEY(room)      REFERENCES Rooms(id),
  PRIMARY KEY(server, room)
);