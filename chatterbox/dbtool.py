import db
import logging

# This tool creates a fresh database and tables
def main():
    logging.basicConfig(level=logging.DEBUG)
    
    db1 = db.ChatterDb()
    db1.openConn()
    print 'Database opened'
    db1.createTables()
    print 'Tables created.'
    db1.closeConn()
    print 'Database closed.'
    
if __name__ == "__main__":
    main()
