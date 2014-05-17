from hashlib import sha1, sha256
import logging
import sqlite3
import time

logger = logging.getLogger('db')

# constants
SQLITE_DB_PATH = 'db/chatterbox.sqlite3'
SESSION_TIMEOUT = 6000

# user table queries
USERS_CREATE_TABLE = 'CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, alias TEXT)'
USERS_REGISTER_USER = 'INSERT INTO users (username, password, alias) VALUES (?, ?, ?)'
USERS_GET_ALIAS = 'SELECT alias FROM users WHERE username = ? AND password = ?'
USERS_UPDATE_ALIAS = 'UPDATE users SET alias = ? WHERE username = ? AND password = ?'

# event table queries
EVENTS_CREATE_TABLE = 'CREATE TABLE IF NOT EXISTS events (eid INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, txt TEXT)'
EVENTS_INSERT_NEW = 'INSERT INTO events (ts, txt) VALUES (?, ?)'
EVENTS_GET_ALL = 'SELECT ts, txt FROM events ORDER BY eid ASC'
EVENTS_CLEAN_TABLE = 'DELETE FROM events WHERE ts < ?'

# session table queries
SESSION_CREATE_TABLE = 'CREATE TABLE IF NOT EXISTS session (id TEXT PRIMARY KEY, username TEXT, ts INTEGER)'
SESSION_CLEAN_TABLE = 'DELETE FROM session WHERE ts < ?'
SESSION_INSERT_NEW = 'INSERT OR REPLACE INTO session (id, username, ts) VALUES (?, ?, ?)'
SESSION_GET = 'SELECT session.username, users.alias FROM session, users WHERE session.id = ? AND session.ts >= ? AND session.username = users.username'

class ChatterDb:
    conn = None 
    
    def openConn(self):
        "This function opens the database. Errors are catched and logged."
        if not self.conn:
            try:
                self.conn = sqlite3.connect(SQLITE_DB_PATH)
            except Exception:
                logger.error('Failed to open database')
        else:
            logger.error('Database already opened!')
    
    def createTables(self):
        "Creates tables in the database if they do not exist."
        if self.conn:
            try:
                cur = self.conn.cursor()
                cur.execute(USERS_CREATE_TABLE)
                cur.execute(EVENTS_CREATE_TABLE)
                cur.execute(SESSION_CREATE_TABLE)
                cur.close()
                self.conn.commit()
            except Exception:
                logger.error('Failed to create tables')
        else:
            logger.error('Database not opened!')
    
    def loginOrRegister(self, username, password, alias=None):
        "Logs in a user and (optionally) registers the user with the system"
        if self.conn:
            cur = self.conn.cursor()
            
            # hash password using SHA1
            password = sha1(password).hexdigest()            
            
            # first, try to register the user
            try:
                cur.execute(USERS_REGISTER_USER, (username, password, alias))
            except sqlite3.IntegrityError:   
                if alias and len(alias) > 0:
                    cur.execute(USERS_UPDATE_ALIAS, (alias, username, password))
                    if cur.rowcount <= 0:
                        alias = None
                else:
                    cur.execute(USERS_GET_ALIAS, (username, password))
                    r = cur.fetchone()
                    if not r is None:
                        alias = r[0]
                    else:
                        alias = None
                    
            cur.close()    
            if alias:
                self.conn.commit();
            else:
                self.conn.rollback();
            
            return alias                    
        else:
            logger.error('Database not opened!')
            return None
    
    def insertEvent(self, txt, ts=None):
        "Creates a new event in the events table"
        if self.conn:
            if not ts:
                ts = int(time.time())
            
            cur = self.conn.cursor()            
            # remove old events
            cleanTs = ts - (SESSION_TIMEOUT*2)
            cur.execute(EVENTS_CLEAN_TABLE, (cleanTs,))            
            
            # insert new event
            cur.execute(EVENTS_INSERT_NEW, (ts, txt))
            cur.close()
            self.conn.commit()
            return True
        else:
            logger.error('Database not opened!')
            return False
    
    def getEvents(self):
        "Retrieve all events from table"
        if self.conn:            
            cur = self.conn.cursor() 
            cur.execute(EVENTS_GET_ALL)
            r = cur.fetchall()
            cur.close()
            return r
        else:
            logger.error('Database not opened!')
            return None
    
    def createSession(self, username, salt):
        "Creates a new session entry in the session table. Returns the session id."
        if self.conn:
            # create unique session id
            sessionId = sha256('%s:%s' % (username, str(salt))).hexdigest()
            ts = int(time.time())
            cur = self.conn.cursor()
            
            # remove old sessions
            cleanTs = ts - SESSION_TIMEOUT
            cur.execute(SESSION_CLEAN_TABLE, (cleanTs,))
            
            # insert new session
            cur.execute(SESSION_INSERT_NEW, (sessionId, username, ts))
            
            cur.close()
            self.conn.commit()
            return sessionId
        else:
            logger.error('Database not opened!')
            return None
    
    def getUserFromSession(self, sessionId):
        "Retrieves the user name from session id"
        if self.conn:
            cur = self.conn.cursor()
            validTs = int(time.time()) - SESSION_TIMEOUT
            
            # get user name from session
            cur.execute(SESSION_GET, (sessionId, validTs))
            r = cur.fetchone()
            cur.close()
            
            if r:
                return (r[0], r[1])
            else:
                return None
        else:
            logger.error('Database not opened!')
            return None
    
    def closeConn(self):
        "Closes the connection if it is opened."
        if self.conn:
            try:
                self.conn.close()
                self.conn = None
            except Exception:
                logger.error('Failed to close database!')
        else:
            logger.error('Database already closed!')

