 while ! sudo -S docker exec eilaji-postgres pg_isready -U eilaji -d eilaji_db >/dev/null 2>&1; do                     
    echo "waiting for DB..."                                                                                            
    sleep 2
  done                                                                                                                  
  echo "✅ DB ready"
