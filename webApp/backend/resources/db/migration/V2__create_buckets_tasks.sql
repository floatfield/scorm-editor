create table bucketsTasks (
    id uuid PRIMARY KEY,
    name VARCHAR NOT NULL,
    maxItemsNumber integer NOT NULL,
    description text NOT NULL,
    bucketNames text[] NOT NULL,
    fullBuckets text NOT NULL
);