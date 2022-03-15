# import
import gurobipy as gb
from gurobipy import GRB



# example graph
B = [1,2,3,4,5,6,7,8,9,10,11,12,13,14] # blocks
E = [(1,2), (1,3),(2,4), (3,4), (4,5), (5,6), (5,7), (5,8), (6,9), (7,9),(8,9),
     (9,10),(10,11), (11, 12), (11,13), (12,14), (13,14)] # edges

# train data
R = [r for r in range(3)] # trains
RR = R.copy()
RR.append(9991) # artificial first ...
RR.append(9999) # ... and last train
timehorizon = 120
T = range(timehorizon) # time in minutes
S = ['S1','S2','S3'] # stations ...
SB = {'S1': [2,3], 'S2': [6,7,8], 'S3' : [12,13]} # ... and their respective blocks

O = {} # stops for each train
for r in R:
    O[r] = S

t=0
A = {} # arrival times of trains for stations
for s in S:
    if s == 'S1':
        t=5
    elif s == 'S2':
        t=20
    elif s == 'S3':
        t=40
    for b in SB[s]:
        for r in R:
            A[r, b] = t+r*5

t=0
D = {} # departure times of trains for stations
for s in S:
    if s == 'S1':
        t=10
    elif s == 'S2':
        t=25
    elif s == 'S3':
        t=45
    for b in SB[s]:
        for r in R:
            D[r, b] = t+r*5

M = {} # minimal travel time on a block b for a train r
for r in R:
    for b in B:
        M[r,b] = 5

# model
model = gb.Model('ttp')

# variables
u = model.addVars(R, B, vtype=GRB.BINARY, name='u') # use block b or not
a = model.addVars(R,B, vtype=GRB.INTEGER, lb=0, name='a') # arrival time for block b
d = model.addVars(R,B, vtype=GRB.INTEGER, lb=0, name='d') # departure time for block b
x = model.addVars(RR,RR,B, vtype=GRB.BINARY, name='x') # train r2 directly follows train r1 on block b

# constraints

# constraint 1: departure time is arrival time in the next block
for r in R:
    for b in B[:-1]:
        model.addConstr(d[r,b] == sum(a[r,bb] for bb in B if (b,bb) in E), name="1 a = d")

# constraint 2: time spent on block is atleast M_rb
for r in R:
    for b in B:
        model.addConstr(a[r,b] + M[r,b] * u[r,b] <= d[r,b], name="2timespent")

# constraint 3: variable linking of a and u
for r in R:
    for b in B:
        model.addConstr(a[r,b] <= u[r,b] * timehorizon, name="3linking a and u")

# constraint 4: enforce visiting of train stations
for r in R:
    for s in O[r]:
        model.addConstr(sum(u[r,b] for b in SB[s]) == 1, name='4visiting')

# constraint 5: flow conservation
for r in R:
    for b in B[:-1]:
        model.addConstr(u[r,b] <= sum(u[r,bb] for bb in B if (b,bb) in E), name='5flowconservation')
"""
# constraint 6: arrival constraints
for r in R:
    for s in O[r]:
        for b in SB[s]:
            model.addConstr(a[r,b] <= A[r,b]* u[r,b], name='6arrival')

# constraint 7: departure constraints
for r in R:
    for s in O[r]:
        for b in SB[s]:
            model.addConstr(D[r, b] * u[r,b]  <= d[r, b], name="7departure")
"""

# constraint 8: tight arrival constraint if trains follow each other
for r in R:
    for rr in R:
        if r != rr:
            for b in B:
                model.addConstr(d[r,b] <= a[rr,b] + (1-x[r,rr,b])* timehorizon, name="8followingtrain" )

# constraint 9: only one successor for each train
for r in RR:
    for b in B:
        model.addConstr(sum(x[r,rr,b] for rr in RR if r != rr) ==1, name="9onesuccessor")

#constraint 10: only one predecessor for each train
for rr in RR:
    for b in B:
        model.addConstr(sum(x[r,rr,b] for r in RR if r != rr) ==1,name="10onepredecessor")


model.optimize()
model.computeIIS()
model.write('model.ilp')

# print output in a readable format


for r in R:
    print('train ' + str(r))
    for b in B:
        if u[r,b].x > 0.5:
            print('block ' + str(b))
            print(a[r,b])
            print(d[r,b])
            print('')
    print('')