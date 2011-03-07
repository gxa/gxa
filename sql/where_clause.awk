BEGIN { 
	comment = 0;
	desired = table;
	table = "";
	sql = "";
}
/\/\*/ { comment = 1 }
/^SELECT \* FROM A2_/ {
	name = $4;
	sub(/A2_/, "", name);
	table = name;
} 
!comment && tolower(table) == tolower(desired) { 
	if (!sql) {
		sql = substr($0, index($0, "A2_" table) + length("A2_" table) + 1);
	} else {
		sql = sql $0;
	}
}
/\;$/ && sql { 
	table = "";
	gsub(/[[:space:]\r\n;]+/, " ", sql);
	print sql;
	sql = 0;
}
/\*\// { comment = 0 }
